const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const { spawnSync } = require('node:child_process')
const { test } = require('node:test')
const { EXPECTED_CHECKS } = require('./results')

const fixture = process.env.REVIEW_RESULTS_FIXTURE
  ? JSON.parse(fs.readFileSync(process.env.REVIEW_RESULTS_FIXTURE, 'utf8').replace(/^\uFEFF/, '')).records
  : EXPECTED_CHECKS.map(name => `REVIEW ${name} PASS`)

function verify(records, expectedExit, field) {
  const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'bukkit-results-'))
  try {
    const input = path.join(temp, 'input.json')
    const output = path.join(temp, 'output.json')
    fs.writeFileSync(input, JSON.stringify({ version: '1.21.11', records }))
    const child = spawnSync(process.execPath, [path.join(__dirname, 'results.js'), input, output], { encoding: 'utf8' })
    assert.equal(child.status, expectedExit, child.stderr || child.stdout)
    const saved = JSON.parse(fs.readFileSync(output, 'utf8'))
    assert.deepEqual(saved.records, records)
    assert.equal(saved.exitCode, expectedExit)
    assert.equal(saved.validation.ok, expectedExit === 0)
    assert.equal(saved.validation.expectedCount, 26)
    if (field) assert.ok(saved.validation[field].length > 0)
    else assert.equal(saved.validation.passedCount, 26)
  } finally {
    fs.rmSync(temp, { recursive: true, force: true })
  }
}

test('all 26 expected checks save success and exit zero', () => verify(fixture, 0))
test('missing assertion fails even without a FAIL record', () => verify(fixture.filter(r => r !== 'REVIEW imbue_stack PASS'), 1, 'missing'))
test('ERROR after all passes still fails', () => verify([...fixture, 'REVIEW ERROR injected exception'], 1, 'errors'))
test('FAIL after all passes still fails', () => verify([...fixture, 'REVIEW buy_success FAIL'], 1, 'failures'))
test('duplicate assertion cannot inflate the pass count', () => verify([...fixture, 'REVIEW buy_success PASS'], 1, 'duplicates'))
test('unexpected assertion cannot substitute for the expected set', () => verify([...fixture, 'REVIEW unknown_check PASS'], 1, 'unexpected'))
