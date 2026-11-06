// The text of the push notifications. This file has no Firebase code, so it is easy to test.

const TIME_ZONE = "Asia/Dhaka";

/**
 * Looks at an application before and after a change and says what to tell the student.
 * Returns null when nobody needs to be told (for example when the student changed it).
 */
function buildMessage(before, after) {
  if (!before || !after || before.status === after.status) {
    return null;
  }

  const job = after.internshipTitle || "your application";
  const company = after.companyName || "the company";

  switch (after.status) {
    case "shortlisted":
      return {
        title: "You are shortlisted",
        body: `${company} shortlisted you for ${job}.`,
      };
    case "interview":
      return {
        title: "Interview planned",
        body: `${company} planned an interview for ${job}${formatTime(after.interviewAt)}.`,
      };
    case "offer":
      return {
        title: "You got an offer",
        body: `${company} sent you an offer for ${job}. Congratulations!`,
      };
    case "rejected":
      return {
        title: "Application update",
        body: `${company} did not choose you for ${job}. Keep going, the next one can be yours.`,
      };
    default:
      // saved and applied are steps of the student
      return null;
  }
}

/**
 * The notification for a new chat message. Returns null when nobody needs it: a system
 * notice ("Interview set") already has its own notification, and the sender is not told.
 * Returns {recipientId, title, body}.
 */
function buildChatMessage(chat, message) {
  if (!chat || !message || message.type === "system" || !message.text) {
    return null;
  }
  const participants = chat.participants || [];
  const recipientId = participants.find((uid) => uid !== message.senderId);
  if (!recipientId) {
    return null;
  }
  const senderName = (chat.names && chat.names[message.senderId]) || "New message";
  const text = message.text.length > 120 ? `${message.text.substring(0, 117)}...` : message.text;
  return {recipientId, title: senderName, body: text};
}

/** " on Tue, 20 Oct, 11:00 AM", or an empty text when there is no time. */
function formatTime(milliseconds) {
  if (!milliseconds) {
    return "";
  }
  const text = new Date(milliseconds).toLocaleString("en-GB", {
    timeZone: TIME_ZONE,
    weekday: "short",
    day: "numeric",
    month: "short",
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  });
  return ` on ${text}`;
}

module.exports = {buildMessage, buildChatMessage};
