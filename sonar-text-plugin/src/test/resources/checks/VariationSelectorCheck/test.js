// Normal JavaScript file
// `cat -A` shows: "// M-oM-8M-^@M-oM-8M-^@ end"; below threshold -> compliant
var x = "hello"; // ︀︀ end
// `cat -A` shows: "M-sM- M-^DM-^@M-sM- M-^DM-^@M-sM- M-^DM-^@M-sM- M-^DM-^@ end "
var y = "world"; // 󠄀󠄀󠄀󠄀 end
console.log("clean");

const payload = "a󠅡󠅬󠅥󠅲󠅴󠄨󠄱󠄩"; // Noncompliant: consecutive variation selectors encode a hidden payload
