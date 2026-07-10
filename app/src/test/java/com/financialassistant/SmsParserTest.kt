package com.financialassistant

import com.financialassistant.sms.filters.SenderFilter
import com.financialassistant.sms.models.TransactionType
import com.financialassistant.sms.parsers.KcbParser
import com.financialassistant.sms.parsers.MpesaParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun testSenderFilter() {
        assertTrue(SenderFilter.isFinancialSender("MPESA"))
        assertTrue(SenderFilter.isFinancialSender("KCB-Alert"))
        assertEquals("M-PESA", SenderFilter.getProviderName("M-PESA"))
        assertEquals("KCB", SenderFilter.getProviderName("KCBBANK"))
    }

    @Test
    fun testMpesaSentParsing() {
        val parser = MpesaParser()
        val sms = "QW12345678 Confirmed. Ksh500.00 sent to JOHN MWANGI 0712345678 on 9/7/26 at 6:46 PM. New M-PESA balance is Ksh1,200.00. Transaction cost, Ksh5.00."
        val transaction = parser.parse(sms, 1773072000000L)

        assertNotNull(transaction)
        assertEquals("QW12345678", transaction?.receiptNumber)
        assertEquals(500.0, transaction?.amount ?: 0.0, 0.0)
        assertEquals("JOHN MWANGI", transaction?.merchant)
        assertEquals(1200.0, transaction?.balance ?: 0.0, 0.0)
        assertEquals(5.0, transaction?.fee ?: 0.0, 0.0)
        assertEquals(TransactionType.SENT, transaction?.transactionType)
    }

    @Test
    fun testMpesaReceivedParsing() {
        val parser = MpesaParser()
        val sms = "QX87654321 Confirmed. You have received Ksh1,200.00 from SARAH WANJIKU 0787654321 on 9/7/26 at 6:50 PM. New M-PESA balance is Ksh2,400.00. Transaction cost, Ksh0.00."
        val transaction = parser.parse(sms, 1773072000000L)

        assertNotNull(transaction)
        assertEquals("QX87654321", transaction?.receiptNumber)
        assertEquals(1200.0, transaction?.amount ?: 0.0, 0.0)
        assertEquals("SARAH WANJIKU", transaction?.merchant)
        assertEquals(2400.0, transaction?.balance ?: 0.0, 0.0)
        assertEquals(0.0, transaction?.fee ?: 0.0, 0.0)
        assertEquals(TransactionType.RECEIVED, transaction?.transactionType)
    }

    @Test
    fun testKcbDebitParsing() {
        val parser = KcbParser()
        val sms = "KCB Alert: Txn: DR. Ksh 2,500.00 from A/C XXXXXX1234 to NAIVAS. Fee: Ksh 30.00. Ref: KCB987654. Balance: Ksh 45,000.00 on 2026-07-09."
        val transaction = parser.parse(sms, 1773072000000L)

        assertNotNull(transaction)
        assertEquals("KCB987654", transaction?.receiptNumber)
        assertEquals(2500.0, transaction?.amount ?: 0.0, 0.0)
        assertEquals("NAIVAS", transaction?.merchant)
        assertEquals(45000.0, transaction?.balance ?: 0.0, 0.0)
        assertEquals(30.0, transaction?.fee ?: 0.0, 0.0)
        assertEquals(TransactionType.SENT, transaction?.transactionType)
    }
}
