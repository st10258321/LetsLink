//package com.example.API_related
//import com.example.letslink.SessionManager
//import com.example.letslink.model.Group
//import com.example.letslink.model.GroupResponse
//import kotlinx.coroutines.flow.Flow
//import java.util.UUID
//// import kotlin.uuid.toKotlinUuid // Removed unused import
//
///**
// *
// * This class hanndels coordination between the local Room database via GroupDao and the
// * remote API (LetsLinkAPI).
// *
// */
//class GroupRepo(
//    private val groupDao: GroupDao,
//    private val groupApiService: LetsLinkAPI,
//    private val sessionManager: SessionManager
//) {
//
//
//    fun getGroupsByUserId(userId: UUID): Flow<List<Group>> {
//        //  DAO's existing method to convert the UUID to a String
//        return groupDao.getNotesByUserId(userId.toString())
//    }
//
//    /**
//     * Handles the creation of a group by saving it locally first,
//     * then synchronizing with the remote API, and then updating it
//     */
//    suspend fun createAndSyncGroup(group: Group): GroupResponse? {
//        //  Save locally first
//        groupDao.insertGroup(group)
//
//        // 2.calling grouprequest which hanldes the sending request to api
//        val apiRequest = GroupRequest(
//            groupId = group.groupId.toString()
//        )
//
//        // 3. Sync with the API
//        return try {
//            val response = groupApiService.createGroup(apiRequest)
//
//            val updatedGroup = group.copy(inviteLink = response.inviteLink)
//            groupDao.insertGroup(updatedGroup)
//            println("Group synced successfully. Invite link received: ${response.inviteLink}")
//            // Return the successful response which means the link has been generated
//            response
//        } catch (e: Exception) {
//            e.printStackTrace()
//            println("ERROR: Failed to sync group ${group.groupId} with API.")
//            // Return null on failure
//            null
//        }
//    }
//
//    /**
//     * Attempts to join an existing group via the API and a new group
//     * entity locally if successful.
//     *
//     */
//    suspend fun joinGroup(groupId: String,userID: UUID): GroupResponse? {
//
//        // Use SessionManager to get the current user ID
//        val currentUserId = sessionManager.getUserId()
//
//        // 1. Prepare the network request
//        val apiRequest = JoinGroupRequest(
//            groupId = groupId,
//            userId = currentUserId
//        )
//
//        // 2. Call the API
//        return try {
//            val response = groupApiService.joinGroup(apiRequest)
//
//            println("DEBUG API RESPONSE: $response")
//
//            val newGroup = Group(
//                groupId = response.groupId,
//                userId = currentUserId,
//                groupName = response.groupName,
//                description = response.description,
//                inviteLink = response.inviteLink
//            )
//
//            groupDao.insertGroup(newGroup)
//
//            println("Group joined successfully: ${response.groupName}")
//            response
//        } catch (e: Exception) {
//            e.printStackTrace()
//            println("ERROR: Failed to join group $groupId. ${e.message}")
//            null
//        }
//    }
//}