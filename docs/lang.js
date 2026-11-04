/* 中英切换：只用一行 localStorage，不引任何框架。
   默认中文（主要用户在国内），选择了英文就记住。 */
(function () {
  var KEY = "ascentroam.lang";
  var body = document.body;
  var btn = document.getElementById("langBtn");

  function apply(lang) {
    body.setAttribute("data-lang", lang);
    if (btn) btn.textContent = lang === "zh" ? "EN" : "中文";
    document.documentElement.setAttribute("lang", lang === "zh" ? "zh-CN" : "en");
  }

  var saved = null;
  try {
    saved = localStorage.getItem(KEY);
  } catch (e) {
    /* 隐私模式禁用存储时按默认语言走，不报错 */
  }
  apply(saved === "en" ? "en" : "zh");

  if (btn) {
    btn.addEventListener("click", function () {
      var next = body.getAttribute("data-lang") === "zh" ? "en" : "zh";
      apply(next);
      try {
        localStorage.setItem(KEY, next);
      } catch (e) {}
    });
  }
})();
