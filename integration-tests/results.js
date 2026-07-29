const fs = require('node:fs')

const EXPECTED_CHECKS = Object.freeze([
  'registered_illagerinvasion:imbuing_table', 'registered_illagerinvasion:magic_fire',
  'buy_permission', 'buy_success', 'buy_insufficient', 'buy_provider_failure',
  'buy_missing_provider', 'sell_success', 'sell_refund_70', 'sell_exact_metadata',
  'gui_success', 'gui_fail', 'guard_shift', 'guard_pickup', 'shulker_open',
  'shulker_no_nesting', 'shulker_conserve', 'shulker_denied', 'cooldown_protocol',
  'nestedoff', 'nestedon', 'race_replace', 'race_drop', 'race_move',
  'imbue_collect', 'imbue_stack'
])

function evaluateRecords(records) {
  const counts = new Map()
  const failures = []
  const errors = []
  if (!Array.isArray(records)) records = ['REVIEW ERROR records must be an array']
  for (const record of records) {
    if (typeof record !== 'string') { errors.push('Non-string result record'); continue }
    if (/\bFAIL\b/.test(record)) failures.push(record)
    if (/\bERROR\b/.test(record)) errors.push(record)
    const pass = /^REVIEW (\S+) PASS$/.exec(record)
    if (pass) counts.set(pass[1], (counts.get(pass[1]) || 0) + 1)
  }
  const missing = EXPECTED_CHECKS.filter(name => !counts.has(name))
  const duplicates = [...counts].filter(([, count]) => count > 1).map(([name]) => name)
  const unexpected = [...counts.keys()].filter(name => !EXPECTED_CHECKS.includes(name))
  const ok = !missing.length && !duplicates.length && !unexpected.length && !failures.length && !errors.length
  return { ok, expectedCount: EXPECTED_CHECKS.length, passedCount: [...counts.values()].reduce((a, b) => a + b, 0), missing, duplicates, unexpected, failures, errors }
}

function saveResults(path, version, records) {
  const validation = evaluateRecords(records)
  const result = { version, records, validation, exitCode: validation.ok ? 0 : 1 }
  fs.writeFileSync(path, JSON.stringify(result, null, 2) + '\n')
  return result
}

module.exports = { EXPECTED_CHECKS, evaluateRecords, saveResults }

if (require.main === module) {
  try {
    const [input, output] = process.argv.slice(2)
    if (!input || !output) throw new Error('Usage: node results.js input.json output.json')
    const source = JSON.parse(fs.readFileSync(input, 'utf8').replace(/^\uFEFF/, ''))
    const result = saveResults(output, source.version, source.records)
    console.log(JSON.stringify(result.validation))
    process.exitCode = result.exitCode
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
