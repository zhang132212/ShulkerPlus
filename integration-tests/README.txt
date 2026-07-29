Isolated Bukkit integration driver, 2026-10-15

Never install ReviewHarness on a production or shared Minecraft server. It exposes
test setup commands, a controlled economy provider, and a shutdown command without
authentication. It is only for the loopback-only offline review instance.

Verified environment:
  Test host: Windows 11
  Server: Paper 1.21.11 build 132, Java 21 (Dragonwell 21.0.10)
  Root: a new dedicated test server directory
  Bind: 127.0.0.1:26901, separate newly generated review-world
  Plugins: EnchantPlus 1.0.0, ShulkerPlus 3.3.14, MenuScrollGuard 1.3.1,
    IllagerInvasionLeaves 1.0.0, Vault 1.7.3-b131,
    ProtocolLib 5.5.0-SNAPSHOT-d89a5de, CraftEngine 26.7.1,
    ModelEngine R4.1.0 build 2364, ReviewHarness 1.0
  Client: Node 24 + Mineflayer 4.39.0, protocol 1.21.11, offline ReviewBot

Reproduction:
1. Use a new dedicated server directory and loopback bind. Accept EULA as required.
2. Install the listed plugin jars, with the reviewed jars built from current source.
3. Initialize CraftEngine normally before copying the illager_invasion resource
   module; it needs its bundled internal and default_templates modules. In this
   test environment these were extracted from the exact CraftEngine jar, rather
   than copied from any running server. Copy the project's ModelEngine blueprints.
4. Run mvn package in this directory with Java 21, then copy target/ReviewHarness.jar
   into the isolated server's plugins directory. The harness uses the actual loaded Vault interface and
   EconomyResponse through reflection, not a replacement API jar.
5. Start Paper with -Xms512M -Xmx2G -jar paper.jar --nogui. Wait for Done and all
   expected REVIEW_PLUGIN enabled flags. For these tests the bot does not need
   a resource pack, an external economy database, or production player data.
6. Run npm ci in the directory with package.json and package-lock.json. If running
   the client locally against a remote host, create an SSH forward for loopback 26901.
7. Run node test.js. It joins the isolated server, prints PASS/FAIL checks and
   snapshots, writes client-results.json, and requests a clean server shutdown.
   Exit code 0 requires exactly one PASS for each of the 26 expected names, with
   no FAIL, ERROR, duplicate or unexpected assertions. client-results.json stores
   the validation details and matching exitCode. Exceptions and timeouts also
   save failed results.

Result-validation regression tests require only Node, without a Minecraft server:
  node --test results.test.js
Set REVIEW_RESULTS_FIXTURE to an existing client-results.json to validate against
real recorded results, including missing/ERROR/FAIL/duplicate/unexpected variants.
To recheck a result file and save a validated copy with a matching process status:
  node results.js original-results.json validated-results.json

Coverage details:
- Actual Bukkit command routing on the server thread, actual player inventory,
  actual registered Vault service; controlled success/failure responses.
- Actual inventory and plugin-message packets sent by a logged-in client,
  including two purchase clicks in the same batch, QUICK_MOVE and PICKUP_ALL.
- Deferred-open races are deterministic: a test event listener changes the source
  inventory after the real right-click event but before next tick opens the item.
- The imbuing session is constructed reflectively using the real plugin class,
  live CraftEngine items and the real Bukkit inventory; the client clicks normally.
  This avoids placing/scanning any world block. Physical block interaction, model
  rendering and AI behavior are outside this driver.
- Imbuing collect protection is tested with MenuScrollGuard bypass permission to
  prove that the imbuing listener itself prevents preview collection.

Do not count fixture failures as product regressions:
- Invoking nested Bukkit.dispatchCommand from inside another command handler
  queues execution in Paper's command context. The command test suite therefore
  runs on the next server tick, outside the outer command's context.
- Custom NMS ShulkerBoxMenu has a Bukkit top inventory reported as CHEST. The
  assertion inspects the actual NMS menu class instead of the Bukkit type enum.
- Pre-populating CraftEngine/resources before first startup skips generation of
  bundled defaults. Restore internal/default_templates in the test setup first.

Final code fixes are only QuickShulker message authorization/cooldown/nested
configuration checks and one-item consumption for each imbuing ingredient slot.
