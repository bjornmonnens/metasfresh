package de.metas.banking.impexp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.adempiere.ad.trx.api.ITrx;
import org.compiere.util.DB;
import org.slf4j.Logger;

import de.metas.logging.LogManager;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/*
 * #%L
 * de.metas.banking.base
 * %%
 * Copyright (C) 2019 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
@UtilityClass
public class BankStatementImportTableSqlUpdater
{
	private static final transient Logger logger = LogManager.getLogger(BankStatementImportTableSqlUpdater.class);

	public void updateBankStatemebtImportTable(@NonNull final String whereClause)
	{

		updateBankAccount(whereClause);
		updateCurrency(whereClause);
		updateAmount(whereClause);
		updateValutaDate(whereClause);
		checkPaymentInvoiceCombination(whereClause);
		checkPaymentBPartnerCombination(whereClause);
		checkInvoiceBPartnerCombination(whereClause);
		checkInvoiceBPartnerPaymentBPartnerCombination(whereClause);

		updateBankStatement(whereClause);

		detectDuplicates(whereClause);

	}

	private void updateBankStatement(final String whereClause)
	{

		final StringBuilder sql = new StringBuilder("UPDATE ")
				.append(" I_BankStatement i ")
				.append(" SET C_BankStatement_ID=(SELECT C_BankStatement_ID FROM C_BankStatement s")
				.append(" WHERE i.")
				.append(" C_BP_BankAccount_ID =s.C_BP_BankAccount_ID ")
				.append(" AND i.AD_Client_ID=s.AD_Client_ID) ")
				.append(" WHERE M_Product_ID IS NULL ")
				.append(" COALESCE(i.name, current_date) = s.Name ")
				.append(" AND COALESCE(i.EftStatementReference, '') = COALESCE(s.EftStatementReference, '') ")
				.append(" AND i.StatementDate = s.StatementDate )")
				.append(" WHERE C_BankStatement_ID IS NULL")
				.append(" AND i.I_IsImported<>'Y' ").append(whereClause);
		final int no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);
		logger.info("Product Existing Value={}", no);

	}

	private void updateBankAccount(final String whereClause)
	{
		StringBuilder sql;
		int no;
		sql = new StringBuilder("UPDATE I_BankStatement i "
				+ "SET C_BP_BankAccount_ID="
				+ "( "
				+ " SELECT C_BP_BankAccount_ID "
				+ " FROM C_BP_BankAccount a, C_Bank b "
				+ " WHERE b.IsOwnBank='Y' "
				+ " AND a.AD_Client_ID=i.AD_Client_ID "
				+ " AND a.C_Bank_ID=b.C_Bank_ID "
				+ " AND a.AccountNo=i.BankAccountNo "
				+ " AND (b.RoutingNo=i.RoutingNo "
				+ " OR b.SwiftCode=i.RoutingNo) "
				+ ") "
				+ "WHERE i.C_BP_BankAccount_ID IS NULL "
				+ "AND i.I_IsImported<>'Y' "
				+ "OR i.I_IsImported IS NULL").append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Bank Account (With Routing No)=", no);

		// TODO: What's this?
		// sql = new StringBuilder("UPDATE I_BankStatement i "
		// + "SET C_BP_BankAccount_ID=(SELECT C_BP_BankAccount_ID FROM C_BP_BankAccount a WHERE a.C_BP_BankAccount_ID=").append(p_C_BP_BankAccount_ID);
		// sql.append(" and a.AD_Client_ID=i.AD_Client_ID) "
		// + "WHERE i.C_BP_BankAccount_ID IS NULL "
		// + "AND i.BankAccountNo IS NULL "
		// + "AND i.I_isImported<>'Y' "
		// + "OR i.I_isImported IS NULL").append(whereClause);
		//
		// no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);
		//
		// if (no != 0)
		// {
		// logger.info("Bank Account=", no);
		// }

		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET I_isImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Bank Account, ' "
				+ "WHERE C_BP_BankAccount_ID IS NULL "
				+ "AND I_isImported<>'Y' "
				+ "OR I_isImported IS NULL").append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.warn("Invalid Bank Account=", no);

	}

	private void updateCurrency(final String whereClause)
	{
		StringBuilder sql;
		int no;
		sql = new StringBuilder("UPDATE I_BankStatement i "
				+ "SET C_Currency_ID=(SELECT C_Currency_ID FROM C_Currency c"
				+ " WHERE i.ISO_Code=c.ISO_Code AND c.AD_Client_ID IN (0,i.AD_Client_ID)) "
				+ "WHERE C_Currency_ID IS NULL"
				+ " AND I_IsImported<>'Y'").append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		if (no != 0)
		{
			logger.info("Set Currency=", no);
		}

		sql = new StringBuilder("UPDATE I_BankStatement i "
				+ "SET C_Currency_ID=(SELECT C_Currency_ID FROM C_BP_BankAccount WHERE C_BP_BankAccount_ID=i.C_BP_BankAccount_ID) "
				+ "WHERE i.C_Currency_ID IS NULL "
				+ "AND i.ISO_Code IS NULL").append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Set Currency=", no);

		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Currency,' "
				+ "WHERE C_Currency_ID IS NULL "
				+ "AND I_IsImported<>'E' "
				+ " AND I_IsImported<>'Y'").append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.warn("Invalid Currency=", no);
	}

	private void updateAmount(final String whereClause)
	{
		StringBuilder sql;
		int no;
		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET ChargeAmt=0 "
				+ "WHERE ChargeAmt IS NULL "
				+ "AND I_IsImported<>'Y'").append(whereClause);
		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Charge Amount=", no);

		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET InterestAmt=0 "
				+ "WHERE InterestAmt IS NULL "
				+ "AND I_IsImported<>'Y'").append(whereClause);
		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Interest Amount=", no);

		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET TrxAmt=StmtAmt - InterestAmt - ChargeAmt "
				+ "WHERE (TrxAmt IS NULL OR TrxAmt = 0) "
				+ "AND I_IsImported<>'Y'").append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Transaction Amount=", no);

		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET I_isImported='E', I_ErrorMsg=I_ErrorMsg||'Err=Invalid Amount, ' "
				+ "WHERE TrxAmt + ChargeAmt + InterestAmt <> StmtAmt "
				+ "AND I_isImported<>'Y'").append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Invaid Amount=", no);
	}

	private void updateValutaDate(final String whereClause)
	{
		StringBuilder sql;
		int no;
		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET ValutaDate=StatementLineDate "
				+ "WHERE ValutaDate IS NULL "
				+ "AND I_isImported<>'Y'").append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Valuta Date=", no);
	}

	private void checkPaymentInvoiceCombination(final String whereClause)
	{
		StringBuilder sql;
		int no;
		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'Err=Invalid Payment<->Invoice, ' "
				+ "WHERE I_BankStatement_ID IN "
				+ "(SELECT I_BankStatement_ID "
				+ "FROM I_BankStatement i"
				+ " INNER JOIN C_Payment p ON (i.C_Payment_ID=p.C_Payment_ID) "
				+ "WHERE i.C_Invoice_ID IS NOT NULL "
				+ " AND p.C_Invoice_ID IS NOT NULL "
				+ " AND p.C_Invoice_ID<>i.C_Invoice_ID) ")
						.append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Payment<->Invoice Mismatch=", no);
	}

	private void checkPaymentBPartnerCombination(final String whereClause)
	{
		StringBuilder sql;
		int no;
		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'Err=Invalid Payment<->BPartner, ' "
				+ "WHERE I_BankStatement_ID IN "
				+ "(SELECT I_BankStatement_ID "
				+ "FROM I_BankStatement i"
				+ " INNER JOIN C_Payment p ON (i.C_Payment_ID=p.C_Payment_ID) "
				+ "WHERE i.C_BPartner_ID IS NOT NULL "
				+ " AND p.C_BPartner_ID IS NOT NULL "
				+ " AND p.C_BPartner_ID<>i.C_BPartner_ID) ")
						.append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Payment<->BPartner Mismatch=", no);
	}

	private void checkInvoiceBPartnerCombination(final String whereClause)
	{
		StringBuilder sql;
		int no;
		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'Err=Invalid Invoice<->BPartner, ' "
				+ "WHERE I_BankStatement_ID IN "
				+ "(SELECT I_BankStatement_ID "
				+ "FROM I_BankStatement i"
				+ " INNER JOIN C_Invoice v ON (i.C_Invoice_ID=v.C_Invoice_ID) "
				+ "WHERE i.C_BPartner_ID IS NOT NULL "
				+ " AND v.C_BPartner_ID IS NOT NULL "
				+ " AND v.C_BPartner_ID<>i.C_BPartner_ID) ")
						.append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Invoice<->BPartner Mismatch=", no);
	}

	private void checkInvoiceBPartnerPaymentBPartnerCombination(final String whereClause)
	{
		StringBuilder sql;
		int no;
		sql = new StringBuilder("UPDATE I_BankStatement "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'Err=Invalid Invoice.BPartner<->Payment.BPartner, ' "
				+ "WHERE I_BankStatement_ID IN "
				+ "(SELECT I_BankStatement_ID "
				+ "FROM I_BankStatement i"
				+ " INNER JOIN C_Invoice v ON (i.C_Invoice_ID=v.C_Invoice_ID)"
				+ " INNER JOIN C_Payment p ON (i.C_Payment_ID=p.C_Payment_ID) "
				+ "WHERE p.C_Invoice_ID<>v.C_Invoice_ID"
				+ " AND v.C_BPartner_ID<>p.C_BPartner_ID) ")
						.append(whereClause);

		no = DB.executeUpdateEx(sql.toString(), ITrx.TRXNAME_ThreadInherited);

		logger.info("Invoice.BPartner<->Payment.BPartner Mismatch=", no);

	}

	private void detectDuplicates(final String whereClause)
	{
		StringBuilder sql;
		int no;
		sql = new StringBuilder("SELECT i.I_BankStatement_ID, l.C_BankStatementLine_ID, i.EftTrxID "
				+ "FROM I_BankStatement i, C_BankStatement s, C_BankStatementLine l "
				+ "WHERE i.I_isImported='N' "
				+ "AND s.C_BankStatement_ID=l.C_BankStatement_ID "
				+ "AND i.EftTrxID IS NOT NULL AND "
				// Concatenate EFT Info
				+ "(l.EftTrxID||l.EftAmt||l.EftStatementLineDate||l.EftValutaDate||l.EftTrxType||l.EftCurrency||l.EftReference||s.EftStatementReference "
				+ "||l.EftCheckNo||l.EftMemo||l.EftPayee||l.EftPayeeAccount) "
				+ "= "
				+ "(i.EftTrxID||i.EftAmt||i.EftStatementLineDate||i.EftValutaDate||i.EftTrxType||i.EftCurrency||i.EftReference||i.EftStatementReference "
				+ "||i.EftCheckNo||i.EftMemo||i.EftPayee||i.EftPayeeAccount) ");

		StringBuffer updateSql = new StringBuffer("UPDATE I_Bankstatement "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'Err=Duplicate['||?||']' "
				+ "WHERE I_BankStatement_ID=?").append(whereClause);
		PreparedStatement pupdt = DB.prepareStatement(updateSql.toString(), ITrx.TRXNAME_ThreadInherited);

		PreparedStatement pstmtDuplicates = null;
		no = 0;
		try
		{
			pstmtDuplicates = DB.prepareStatement(sql.toString(), ITrx.TRXNAME_ThreadInherited);
			ResultSet rs = pstmtDuplicates.executeQuery();
			while (rs.next())
			{
				String info = "Line_ID=" + rs.getInt(2) // l.C_BankStatementLine_ID
						+ ",EDTTrxID=" + rs.getString(3); // i.EftTrxID
				pupdt.setString(1, info);
				pupdt.setInt(2, rs.getInt(1)); // i.I_BankStatement_ID
				pupdt.executeUpdate();
				no++;
			}
			rs.close();
			pstmtDuplicates.close();
			pupdt.close();

			rs = null;
			pstmtDuplicates = null;
			pupdt = null;
		}
		catch (Exception e)
		{
			logger.error("DetectDuplicates ", e.getMessage());
		}

		logger.info("Duplicates=", no);

	}

}
