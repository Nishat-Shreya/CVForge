// Run with: npm test   (inside the functions folder)
const test = require("node:test");
const assert = require("node:assert");
const {buildMessage, buildChatMessage} = require("./messages");

const base = {internshipTitle: "Android Intern", companyName: "Nexa Labs"};

test("shortlisted tells the student", () => {
  const message = buildMessage({...base, status: "applied"}, {...base, status: "shortlisted"});
  assert.strictEqual(message.title, "You are shortlisted");
  assert.match(message.body, /Nexa Labs/);
});

test("interview message has the time", () => {
  const message = buildMessage(
      {...base, status: "shortlisted"},
      {...base, status: "interview", interviewAt: Date.UTC(2026, 9, 20, 5, 0)},
  );
  assert.match(message.body, / on /);
});

test("offer and rejected have a message", () => {
  assert.ok(buildMessage({status: "interview"}, {...base, status: "offer"}));
  assert.ok(buildMessage({status: "interview"}, {...base, status: "rejected"}));
});

test("steps of the student give no message", () => {
  assert.strictEqual(buildMessage({status: "saved"}, {...base, status: "applied"}), null);
});

test("no status change gives no message", () => {
  assert.strictEqual(buildMessage({status: "applied"}, {...base, status: "applied", notes: "x"}), null);
});

const chat = {participants: ["a", "b"], names: {a: "Nexa Labs HR", b: "Ayesha"}};

test("a chat message goes to the other person with the name of the sender", () => {
  const result = buildChatMessage(chat, {senderId: "a", text: "Can you join an interview?", type: "text"});
  assert.strictEqual(result.recipientId, "b");
  assert.strictEqual(result.title, "Nexa Labs HR");
  assert.strictEqual(result.body, "Can you join an interview?");
});

test("a long chat message is cut", () => {
  const result = buildChatMessage(chat, {senderId: "b", text: "x".repeat(200), type: "text"});
  assert.strictEqual(result.recipientId, "a");
  assert.ok(result.body.length <= 120);
});

test("a system notice gives no chat notification", () => {
  assert.strictEqual(buildChatMessage(chat, {senderId: "a", text: "Interview set", type: "system"}), null);
});
