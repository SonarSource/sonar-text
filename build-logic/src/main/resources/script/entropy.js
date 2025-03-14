const RandExp = require('randexp');

function getMedian(lst) {
    lst = [...lst].sort((a, b) => a - b);
    const l = lst.length;
    if (l % 2 === 1) {
        return lst[Math.floor(l / 2)];
    } else {
        return (lst[Math.floor(l / 2) - 1] + lst[Math.floor(l / 2)]) / 2;
    }
}

function estimateShannonEntropy(str) {
    const len = str.length

    // Build a frequency map from the string.
    const frequencies = Array.from(str)
        .reduce((freq, c) => (freq[c] = (freq[c] || 0) + 1) && freq, {})

    // Sum the frequency of each character.
    return Object.values(frequencies)
        .reduce((sum, f) => sum - f/len * Math.log2(f/len), 0)
}

function getRegExpFromPattern(pattern) {
    let flags = "";
    if (pattern.startsWith("(?i)")) {
        pattern = pattern.substring(4);
        flags += "i";
    }

    return new RegExp(pattern, flags);
}

function computeEntropy(pattern, samples = 5000) {
    let minimum = null;
    let minSample = null;
    let maximum = null;
    let maxSample = null;
    const measures = [];

    const regexp = getRegExpFromPattern(pattern);
    const randexp = new RandExp(regexp);
    for (let i = 0; i < samples; i++) {
        let sample = randexp.gen();
        const matches = sample.match(regexp);
        if (matches && matches[1]) {
            // If there are capturing groups, compute the entropy of the first capturing group only
            // Same logic is used in sonar-text "PatternMatcher"
            sample = matches[1];
        }
        const entropy = estimateShannonEntropy(sample);
        measures.push(entropy);
        if (minimum === null || entropy < minimum) {
            minimum = entropy;
            minSample = sample;
        }
        if (maximum === null || entropy > maximum) {
            maximum = entropy;
            maxSample = sample;
        }
    }
    measures.sort((a, b) => a - b);
    const median = getMedian(measures);
    let suggestedThreshold = median - median * 0.20;
    console.log(`Minimum entropy: ${minimum}\tMaximum entropy: ${maximum}\tMedian entropy: ${median}`);
    console.log(`Suggested threshold: ${suggestedThreshold}`);
    return suggestedThreshold;
}

module.exports = {computeEntropy}
