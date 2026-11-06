// Cloud Functions for CVForge. The notification text is made in messages.js.

const {onDocumentUpdated, onDocumentCreated} = require("firebase-functions/v2/firestore");
const {logger} = require("firebase-functions");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");
const {buildMessage, buildChatMessage} = require("./messages");

initializeApp();

/**
 * When a company changes the status of an application, the student gets a push notification.
 * The function runs in the same region as the Firestore database (asia-south1).
 * The Gemini key and other keys are never needed here, only the project's own service account.
 */
exports.notifyStatusChange = onDocumentUpdated(
    {document: "applications/{applicationId}", region: "asia-south1"},
    async (event) => {
      const before = event.data.before.data();
      const after = event.data.after.data();

      const message = buildMessage(before, after);
      if (!message) {
        return;
      }

      const userRef = getFirestore().collection("users").doc(after.studentId);
      const token = (await userRef.get()).get("fcmToken");
      if (!token) {
        logger.info("The student has no push token yet", {studentId: after.studentId});
        return;
      }

      try {
        // a data message: the app builds the notification itself, also when it is in the background
        await getMessaging().send({
          token,
          data: {
            title: message.title,
            body: message.body,
            target: `application:${event.params.applicationId}`,
          },
          android: {priority: "high"},
        });
      } catch (error) {
        if (error.code === "messaging/registration-token-not-registered") {
          // the app was removed from that phone: forget the old token
          await userRef.update({fcmToken: FieldValue.delete()});
        } else {
          logger.error("The push notification could not be sent", error);
        }
      }
    },
);

/**
 * When someone sends a chat message, the other person gets a push notification.
 * A tap on it opens the chat list of the app ("chat:ID").
 */
exports.notifyChatMessage = onDocumentCreated(
    {document: "chats/{chatId}/messages/{messageId}", region: "asia-south1"},
    async (event) => {
      const message = event.data.data();
      const chatRef = getFirestore().collection("chats").doc(event.params.chatId);
      const chat = (await chatRef.get()).data();

      const notice = buildChatMessage(chat, message);
      if (!notice) {
        return;
      }

      const userRef = getFirestore().collection("users").doc(notice.recipientId);
      const token = (await userRef.get()).get("fcmToken");
      if (!token) {
        logger.info("The chat partner has no push token yet", {recipientId: notice.recipientId});
        return;
      }

      try {
        await getMessaging().send({
          token,
          data: {
            title: notice.title,
            body: notice.body,
            target: `chat:${event.params.chatId}`,
          },
          android: {priority: "high"},
        });
      } catch (error) {
        if (error.code === "messaging/registration-token-not-registered") {
          await userRef.update({fcmToken: FieldValue.delete()});
        } else {
          logger.error("The chat notification could not be sent", error);
        }
      }
    },
);
