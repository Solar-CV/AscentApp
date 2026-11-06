/**
 * Google tokeninfo 中继（部署在 ap-hongkong，函数 URL 触发）。
 *
 * 为什么需要它：上海环境访问不到 oauth2.googleapis.com（实测 fetch failed），
 * 而 Google ID token 的验签必须由 Google 自己完成。上海的函数把 token 发到这里，
 * 这里转发给 Google 并原样透传状态码与响应体 —— **不做任何解释或缓存**，
 * 校验逻辑仍然只有一处（我们自己的 API）。
 *
 * 安全：需要 `x-relay-key` 头（或 ?key=），值与函数环境变量 RELAY_KEY 一致。
 * 没有它就是一个公开的 token 校验代理，虽然不泄露数据，但会替别人消耗我们的配额。
 */
const RELAY_KEY = process.env.RELAY_KEY || "";
const TOKENINFO = "https://oauth2.googleapis.com/tokeninfo?id_token=";

function respond(code, body, contentType) {
  return {
    statusCode: code,
    headers: { "content-type": contentType || "application/json; charset=utf-8" },
    body,
  };
}

function queryOf(event) {
  if (event && event.queryStringParameters) return event.queryStringParameters;
  if (event && event.queryString) return event.queryString;
  const path = String((event && event.path) || "");
  const q = path.includes("?") ? path.slice(path.indexOf("?") + 1) : "";
  return q ? Object.fromEntries(new URLSearchParams(q)) : {};
}

function headersOf(event) {
  const src = (event && event.headers) || {};
  const out = {};
  for (const k of Object.keys(src)) out[k.toLowerCase()] = src[k];
  return out;
}

exports.main_handler = async (event) => {
  const headers = headersOf(event);
  const query = queryOf(event);
  const key = headers["x-relay-key"] || query.key || "";
  if (RELAY_KEY && key !== RELAY_KEY) return respond(403, JSON.stringify({ error: "forbidden" }));

  const token = query.token || "";
  if (!token) return respond(400, JSON.stringify({ error: "missing_token" }));

  try {
    const r = await fetch(TOKENINFO + encodeURIComponent(token), {
      signal: AbortSignal.timeout(8000),
    });
    const text = await r.text();
    return respond(r.status, text, r.headers.get("content-type") || "application/json; charset=utf-8");
  } catch (e) {
    return respond(
      502,
      JSON.stringify({ error: "upstream_unreachable", detail: String((e && e.message) || e) })
    );
  }
};
