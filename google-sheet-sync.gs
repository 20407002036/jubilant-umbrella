/**
 * Google Sheet income sync (Apps Script web app).
 *
 * Setup:
 *   1. Create or open a Google Sheet.
 *   2. Extensions -> Apps Script and paste this file.
 *   3. Change SHARED_SECRET to a long random value.
 *   4. Deploy -> New deployment -> Web app.
 *        Execute as: Me
 *        Who has access: Anyone
 *   5. Copy the /exec URL and the secret into the app's Settings screen.
 *
 * The script ignores duplicate receipts, so retries from the app are safe.
 */

var SHEET_NAME = 'Income';
var SHARED_SECRET = 'change-me-to-a-long-random-secret';

function doPost(e) {
  var lock = LockService.getScriptLock();
  var locked = false;
  try {
    lock.waitLock(30000);
    locked = true;

    var body = {};
    try {
      body = JSON.parse((e && e.postData && e.postData.contents) || '{}');
    } catch (err) {
      return json_({ ok: false, error: 'invalid-json' });
    }

    if (!body || body.token !== SHARED_SECRET) {
      return json_({ ok: false, error: 'unauthorized' });
    }

    var sheet = getSheet_();
    var receipt = String(body.receiptNumber || '');

    if (receipt && isDuplicate_(sheet, receipt)) {
      return json_({ ok: true, duplicate: true });
    }

    sheet.appendRow([
      new Date(),
      String(body.sender || 'Unknown sender'),
      Number(body.amount || 0),
      String(body.provider || ''),
      String(body.senderPhone || ''),
      receipt,
      body.timestamp ? new Date(Number(body.timestamp)) : new Date()
    ]);

    return json_({ ok: true });
  } catch (err) {
    return json_({ ok: false, error: String(err) });
  } finally {
    if (locked) lock.releaseLock();
  }
}

function doGet() {
  return json_({ ok: true, service: 'income-sync' });
}

function getSheet_() {
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = spreadsheet.getSheetByName(SHEET_NAME);
  if (!sheet) {
    sheet = spreadsheet.insertSheet(SHEET_NAME);
    sheet.appendRow([
      'Logged at',
      'Sender',
      'Amount (KES)',
      'Provider',
      'Sender phone',
      'Receipt',
      'Transaction time'
    ]);
    sheet.setFrozenRows(1);
    sheet.getRange('C2:C').setNumberFormat('#,##0.00');
  }
  return sheet;
}

function isDuplicate_(sheet, receipt) {
  var lastRow = sheet.getLastRow();
  if (lastRow < 2) return false;

  var values = sheet.getRange(2, 6, lastRow - 1, 1).getValues();
  for (var i = 0; i < values.length; i++) {
    if (String(values[i][0]) === receipt) return true;
  }
  return false;
}

function json_(payload) {
  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}
