// Normal JavaScript file
// `cat -A` shows: "// M-oM-8M-^@M-oM-8M-^@ end"
var x = "hello"; // ︀︀ end
// `cat -A` shows: "M-sM- M-^DM-^@M-sM- M-^DM-^@M-sM- M-^DM-^@ end"
var y = "world"; // 󠄀󠄀󠄀 end
// `cat -A` shows: "M-oM-8M-^A"
var z = "safe"; // ︁
console.log("clean");
