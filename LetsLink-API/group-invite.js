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
        databaseURL: databaseURL 
    });

    db = admin.database(); 
    console.log("Firebase Realtime Database initialized successfully.");
} catch (e) {
    console.error("Firebase Initialization Failed:", e.message);
    throw e;
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
//creates the group both in firebase and room
function createGroupResponse(groupId, groupData) {
    const userId = groupData?.userId || 'server-owner-id';
    const groupName = groupData?.groupName || 'Default Group Name';
    const description = groupData?.description || 'A new collaborative group.';
    const members = groupData?.members || [userId];

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
 * */
app.post('/groups', async (req, res) => { 
    const groupId = req.body.groupId;
    const userId = req.body.userId || uuidv4();

    if (!groupId) {
        console.error('Group creation failed: Missing groupId in request body.');
        return res.status(400).json({ error: 'The request body must contain a "groupId" field.' });
    }

    // Check if the group already exists in Firebase
    const groupRef = db.ref('groups/' + groupId);
    const snapshot = await groupRef.once('value'); 

    if (snapshot.exists()) {
        console.log(`Group already exists in Firebase: ${groupId}`);
        const data = snapshot.val(); 
        return res.status(200).json(createGroupResponse(groupId, data)); 
    }

    // produces  new group data
    const newGroupData = {
        userId: userId, 
        groupName: req.body.groupName || `Group ${groupId.substring(0, 4)}`,
        description: req.body.description || `Joined group : ${userId}.`,
        members: [userId] 
    };

    // Write the new group data to Firebase
    await groupRef.set(newGroupData);


    return res.status(201).json(createGroupResponse(groupId, newGroupData)); 

  
});


/**
 * Endpoint for a user to join a group using the invite link/group ID.
 */
app.post('/api/group/join', async (req, res) => {
    const { groupId, userId } = req.body;

    // Validation
    if (!groupId || !userId) {
        return res.status(400).json({ error: 'Missing groupId or userId in request body.' });
    }

    // Read group  from Firebase 
    const groupRef = db.ref('groups/' + groupId);
    const snapshot = await groupRef.once('value');

    if (!snapshot.exists()) {
        console.log(`Join failed: Group ID ${groupId} not found.`);
        return res.status(404).json({ error: `Group ID ${groupId} not found.` });
    }

    let groupData = snapshot.val(); 
    
    // Ensure members is an array for safe use
    if (!Array.isArray(groupData.members)) {
        groupData.members = [];
    }

    // Add user to members list only if not in the group
    if (!groupData.members.includes(userId)) {
        groupData.members.push(userId);
        
        // Update the members array in Firebase
        await groupRef.update({ members: groupData.members }); 

        console.log(`User ${userId} successfully joined group ${groupId}. Total members: ${groupData.members.length}`);


    } else {
        console.log(`User ${userId} is already a member of group ${groupId}.`);
    }

    // Return the complete Group table required by the mobile app's repository

    return res.status(200).json(createGroupResponse(groupId, groupData));
});

/**
 * Endpoint for checking if a group link is valid
 */
app.get('/invite/:groupId', async (req, res) => {
    const groupId = req.params.groupId;
    
    // Read group data from Firebase instead of volatile memory
    const groupRef = db.ref('groups/' + groupId);
    const snapshot = await groupRef.once('value');

    if (snapshot.exists()) {
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
