const express = require('express');
const { v4: uuidv4 } = require('uuid');
const admin = require('firebase-admin'); 
let db;
try {
    const serviceAccountString = process.env.FIREBASE_SERVICE_ACCOUNT_KEY;
    const databaseURL = process.env.FIREBASE_DATABASE_URL; 

    if (!serviceAccountString || !databaseURL) {
        throw new Error("Missing FIREBASE_SERVICE_ACCOUNT_KEY or FIREBASE_DATABASE_URL environment variable.");
    }

    const serviceAccount = JSON.parse(serviceAccountString);

    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      databaseURL: databaseURL // Use the URL from .env that is from render
    });

    db = admin.database(); // <-- get real time db ref
    console.log("Firebase Realtime Database initialized successfully.");
} catch (e) {
    console.error("Firebase Initialization Failed:", e.message);
 
}
const app = express();
const port = process.env.PORT || 3000;

const host = 'letslink-api.onrender.com'; 
app.use(express.json());

//const groups = {};
app.get('/', (req, res) => {
    res.status(200).json({ 
        status: "OK", 
        message: "API is running successfully" 
    });
});

function createGroupResponse(groupId, userId = 'server-owner-id', groupName = 'Default Group Name', description = 'A new collaborative group.') {
    const members = groups[groupId]?.members || [userId];

    return {
        groupId: groupId,
        userId: userId,
        groupName: groupName,
        description: description,
        inviteLink: `https://${host}/invite/${groupId}`,
        members: members
    };
}


/**
 * Endpoint to create a new group. It now requires the groupId to be sent by the app
 * 
 */
app.post('/groups', (req, res) => {
    // The mobile app is responsible for generating and sending the UUID here
    const groupId = req.body.groupId; 
    
    const userId = req.body.userId || uuidv4(); 
    
    // Validation to ensure the app provided the ID
    if (!groupId) {
        console.error('Group creation failed: Missing groupId in request body.');
        return res.status(400).json({ error: 'The request body must contain a "groupId" field.' });
    }
    
    // Checks the Group ID already exists and returns the existing group information.
    if (groups[groupId]) {
        console.log(`Group already exists: ${groupId}`);
        return res.status(200).json(createGroupResponse(
            groupId, 
            groups[groupId].userId, 
            groups[groupId].groupName, 
            groups[groupId].description
        ));
    }

    //Initialise a new group with the old group details
    groups[groupId] = {
        userId: userId, // Stores the owner's ID
        groupName: req.body.groupName || `Group ${groupId.substring(0, 4)}`,
        description: req.body.description || `Joined group : ${userId}.`,
        members: [userId] //tracks the orginal owner
    };

    console.log(`Created new group: ${groupId} (Owner: ${userId})`);

    // Return the new group response with a 201 Created status.
    res.status(201).json(createGroupResponse(groupId, userId, groups[groupId].groupName, groups[groupId].description));
});


/**
 * Endpoint for a user to join a group using the invite link/group ID.
 */
app.post('/api/group/join', (req, res) => {
    const { groupId, userId } = req.body;

    // Validation
    if (!groupId || !userId) {
        return res.status(400).json({ error: 'Missing groupId or userId in request body.' });
    }

    const groupData = groups[groupId];

    if (!groupData) {
        console.log(`Join failed: Group ID ${groupId} not found.`);
        return res.status(404).json({ error: `Group ID ${groupId} not found.` });
    }

    // Add user to members list only if not in the group
    if (!groupData.members.includes(userId)) {
        groupData.members.push(userId);
        console.log(`User ${userId} successfully joined group ${groupId}. Total members: ${groupData.members.length}`);

        console.log("Current Group members:", groups[groupId], "UserId: ", userId);
    } else {
        console.log(`User ${userId} is already a member of group ${groupId}.`);
    }

    // Return the complete Group table required by the mobile app's repository
    res.status(200).json(createGroupResponse(
        groupId, 
        groupData.userId, 
        groupData.groupName, 
        groupData.description
    ));
});

/**
 * Endpoint for checking if a group link is valid
 */
app.get('/invite/:groupId', (req, res) => {
    const groupId = req.params.groupId;
    
    if (groups[groupId]) {
        console.log(`Group ID ${groupId} successfully processed via invite link.`);
        res.status(200).send(`Group link for ID ${groupId} is valid and ready for processing.`);
    } else {
        console.log(`Failed attempt to process invalid group ID: ${groupId}.`);
        res.status(404).send('Invalid group invitation link.');
    }
});


// Start the server
app.listen(port, () => {
    console.log(`Group Invitation API is running and listening on port ${port}`);
    console.log(`App must connect to: http://${host}:${port}`);
});
