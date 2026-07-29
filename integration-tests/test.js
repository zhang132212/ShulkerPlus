const mineflayer = require('mineflayer')
const { once } = require('events')
const fs = require('fs')
const { saveResults } = require('./results')
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))
const bot = mineflayer.createBot({ host: '127.0.0.1', port: 26901, username: 'ReviewBot', auth: 'offline', version: '1.21.11', hideErrors: false })
const records = []
bot.on('messagestr', text => { if (text.includes('REVIEW')) { console.log(text); records.push(text) } })
bot.on('error', error => { console.error(error); records.push('REVIEW ERROR ' + error.message) })
bot.on('kicked', reason => console.log('KICKED', reason))
async function cmd(text, delay=700) { bot.chat('/reviewtest '+text); await sleep(delay) }
function rawClick(slot, mode=0, button=0) {
  bot._client.write('window_click', { windowId: (bot.currentWindow || bot.inventory).id, stateId: -1, slot, mouseButton: button, mode, changedSlots: [], cursorItem: {itemCount: 0, components: [], removeComponents: []} })
}
function quickOpen(slot) {
  const data=Buffer.alloc(4);data.writeInt32BE(slot)
  bot._client.write('custom_payload',{channel:'quickshulker:open_shulker_packet',data})
}
async function main() {
  await once(bot,'spawn'); await sleep(1800)
  console.log('SPAWNED',bot.version)
  await cmd('content')
  await cmd('commands',1500)
  await cmd('gui success'); rawClick(10);rawClick(10);await sleep(500);await cmd('check gui_success')
  await cmd('gui fail');rawClick(10);await sleep(500);await cmd('check gui_fail')
  await cmd('guard');rawClick(0,1);await sleep(500);await cmd('check guard_shift')
  rawClick(0);await sleep(500);await cmd('check guard_pickup')
  await cmd('shulker success');bot.activateItem();await sleep(800);await cmd('check shulker_open')
  rawClick(55,1);await sleep(500);await cmd('check shulker_no_nesting')
  rawClick(0,1);await sleep(500);if(bot.currentWindow)bot.closeWindow(bot.currentWindow);await sleep(600);await cmd('check shulker_conserve')
  await cmd('shulker denied');quickOpen(36);await sleep(800);await cmd('check shulker_denied')
  await cmd('shulker success');quickOpen(36);await sleep(150)
  const firstWindow=bot.currentWindow?.id
  quickOpen(55);await sleep(250)
  const cooldownResult='REVIEW cooldown_protocol '+(firstWindow!==undefined&&bot.currentWindow?.id===firstWindow?'PASS':'FAIL')
  records.push(cooldownResult);console.log(cooldownResult)
  await cmd('shulker nestedoff');bot.activateItem();await sleep(800)
  console.log('NESTED WINDOW',bot.currentWindow?.type,bot.currentWindow?.inventoryStart)
  // QuickShulker's mod channel opens a second inventory item while the workbench is open.
  quickOpen((bot.currentWindow?.inventoryStart ?? 10)+28);await sleep(800);await cmd('check nestedoff')
  await cmd('shulker nestedon');bot.activateItem();await sleep(800)
  quickOpen((bot.currentWindow?.inventoryStart ?? 10)+28);await sleep(800);await cmd('check nestedon')
  for(const mode of ['replace','drop','move']) { await cmd('shulker '+mode);bot.setQuickBarSlot(0);bot.activateItem();await sleep(800);await cmd('check race_'+mode) }
  await cmd('imbue collect');rawClick(27,6);await sleep(500);await cmd('check imbue_collect')
  await cmd('imbue');rawClick(4);await sleep(500);await cmd('check imbue_stack')
  saveResults('client-results.json',bot.version,records)
  await cmd('stop');bot.quit();setTimeout(()=>process.exit(saveResults('client-results.json',bot.version,records).exitCode),1500)
}
main().catch(error=>{console.error(error);records.push('REVIEW ERROR '+error.message);fs.writeFileSync('client-error.txt',String(error.stack));saveResults('client-results.json',bot.version,records);bot.quit();process.exitCode=1})
setTimeout(()=>{console.error('GLOBAL TIMEOUT');records.push('REVIEW ERROR global timeout');const result=saveResults('client-results.json',bot.version,records);bot.quit();process.exit(result.exitCode)},120000).unref()
