import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";

const constantsPath = new URL(
  "../../shared/src/androidMain/kotlin/com/xingheyuzhuan/shiguangschedule/ui/schoolselection/web/AndroidWebConstants.kt",
  import.meta.url,
);
const kotlinSource = fs.readFileSync(constantsPath, "utf8");
const scriptMatch = kotlinSource.match(/val JS_INTERCEPT_POST = """([\s\S]*?)"""\.trimIndent\(\)/);
assert.ok(scriptMatch, "Unable to extract JS_INTERCEPT_POST from AndroidWebConstants.kt");

const registeredBodies = [];
const fetchCalls = [];

function MockXMLHttpRequest() {
  this.nativeHeaders = [];
}
MockXMLHttpRequest.prototype.open = function (method, url) {
  this.nativeMethod = method;
  this.nativeUrl = url;
};
MockXMLHttpRequest.prototype.setRequestHeader = function (header, value) {
  this.nativeHeaders.push([header, value]);
};
MockXMLHttpRequest.prototype.send = function (body) {
  this.nativeBody = body;
};

const window = {
  location: new URL("http://newxk.urp.seu.edu.cn/xsxk/profile/index.html"),
  WebPostService: {
    register(id, body, contentType) {
      registeredBodies.push({ id, body, contentType });
    },
  },
  URLSearchParams,
  fetch(input, init) {
    fetchCalls.push({ input, init });
    return Promise.resolve({ ok: true });
  },
};

vm.runInNewContext(scriptMatch[1], {
  window,
  document: { addEventListener() {} },
  XMLHttpRequest: MockXMLHttpRequest,
  URL,
  URLSearchParams,
  console,
});

const crossOriginXhr = new MockXMLHttpRequest();
crossOriginXhr.open("POST", "https://newxk.urp.seu.edu.cn/xsxk/auth/login");
crossOriginXhr.setRequestHeader("Content-Type", "application/json");
crossOriginXhr.send('{"username":"test"}');
assert.equal(
  crossOriginXhr.nativeHeaders.some(([name]) => name.toLowerCase() === "x-webview-post-id"),
  false,
  "Cross-origin XHR must not carry the internal request-id header",
);
assert.equal(registeredBodies.length, 0, "Cross-origin XHR must not enter the native POST registry");

const sameOriginXhr = new MockXMLHttpRequest();
sameOriginXhr.open("POST", "http://newxk.urp.seu.edu.cn/xsxk/example");
sameOriginXhr.setRequestHeader("Content-Type", "application/json");
sameOriginXhr.send('{"probe":true}');
assert.equal(
  sameOriginXhr.nativeHeaders.some(([name]) => name.toLowerCase() === "x-webview-post-id"),
  true,
  "Same-origin XHR should retain native POST interception",
);
assert.equal(registeredBodies.length, 1, "Same-origin XHR should enter the native POST registry");

await window.fetch("https://newxk.urp.seu.edu.cn/xsxk/auth/login", {
  method: "POST",
  body: '{"username":"test"}',
  headers: { "Content-Type": "application/json" },
});
assert.equal(
  Object.keys(fetchCalls.at(-1).init.headers).some((name) => name.toLowerCase() === "x-webview-post-id"),
  false,
  "Cross-origin fetch must not carry the internal request-id header",
);

await window.fetch("http://newxk.urp.seu.edu.cn/xsxk/example", {
  method: "POST",
  body: '{"probe":true}',
  headers: { "Content-Type": "application/json" },
});
assert.equal(
  Object.keys(fetchCalls.at(-1).init.headers).some((name) => name.toLowerCase() === "x-webview-post-id"),
  true,
  "Same-origin fetch should retain native POST interception",
);

console.log("WebView cross-origin request interception tests passed.");
