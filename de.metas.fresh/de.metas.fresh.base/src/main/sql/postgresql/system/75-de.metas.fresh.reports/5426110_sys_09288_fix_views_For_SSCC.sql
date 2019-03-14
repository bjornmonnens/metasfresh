DROP VIEW IF EXISTS report.fresh_HU_SSCC_Label_Report;

CREATE OR REPLACE VIEW report.fresh_HU_SSCC_Label_Report as
SELECT
	/*
	 * NOTE to developer: please keep in sync with fresh_EDI_DesadvLine_SSCC_Label_Report
	 */
	 
	( 
	SELECT  COALESCE(org_bp.name, '') || ', ' || COALESCE(org_l.Postal, '')|| ' '  || COALESCE(org_l.city, '')
		FROM AD_Org org
		
		-- INFO I NEED
		INNER JOIN AD_OrgInfo org_info ON org.AD_Org_ID = org_info.AD_Org_ID
		INNER JOIN C_BPartner_Location org_bpl ON org_info.orgBP_Location_ID = org_bpl.C_BPartner_Location_ID
		INNER JOIN C_Location org_l ON org_bpl.C_Location_ID = org_l.C_Location_ID
		INNER JOIN c_bpartner org_bp ON org_bpl.C_Bpartner_ID = org_bp.C_BPartner_ID
		
		-- LINKING
		INNER JOIN  C_OrderLine ol ON ol.AD_Org_ID = org.AD_Org_ID
		INNER JOIN M_ReceiptSchedule rs ON ol.C_OrderLine_ID = rs.Record_ID
		INNER JOIN M_HU_Trx_Line hutl ON rs.M_ReceiptSchedule_ID = hutl.Record_ID
		
		WHERE hutl.M_HU_ID =lu.M_HU_ID
	) as org_address,
	lua_sscc.value AS SSCC,
	bpp.productno AS p_customervalue,
	(
		SELECT 	avg(ol.priceactual)
		FROM	M_HU_Trx_Line hutl
			INNER JOIN M_ReceiptSchedule rs ON hutl.Record_ID = rs.M_ReceiptSchedule_ID
			INNER JOIN C_OrderLine ol ON rs.Record_ID = ol.C_OrderLine_ID
		WHERE	hutl.M_HU_ID = lu.M_HU_ID
			AND hutl.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'M_ReceiptSchedule')
			AND rs.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'C_OrderLine')
	)  AS PriceActual,
	COALESCE( pt.name, p.name ) AS p_name,
	qty.CU_per_TU,
	qty.TU_per_LU,
	lua_netw.valuenumber as net_weight,
	lua_grow.valuenumber as gross_weight,
	(
		SELECT 	max(o.documentno)
		FROM	M_HU_Trx_Line hutl
			INNER JOIN M_ReceiptSchedule rs ON hutl.Record_ID = rs.M_ReceiptSchedule_ID
			INNER JOIN C_OrderLine ol ON rs.Record_ID = ol.C_OrderLine_ID
			INNER JOIN C_Order o ON ol.C_Order_ID = o.C_Order_ID
		WHERE	hutl.M_HU_ID = lu.M_HU_ID
			AND hutl.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'M_ReceiptSchedule')
			AND rs.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'C_OrderLine')
	) AS order_docno,
	p.value AS P_Value,
	lua_lot.value AS LotCode,
	lu.value AS PaletNo,
	COALESCE(bp.name, '') || ' ' || COALESCE(bpl.name, '') || E'\n' AS Customer,
	-- Adding language. This language is used for the captions in the report 
	bp.ad_Language,
	--
	-- Filtering fields
	lu.M_HU_ID
FROM
	M_HU lu
	INNER JOIN M_HU_Storage lus ON lu.M_HU_ID = lus.M_HU_ID
	LEFT OUTER JOIN C_BPartner bp ON lu.C_BPartner_ID = bp.C_BPartner_ID
	LEFT OUTER JOIN C_BPartner_Location bpl ON lu.C_BPartner_Location_ID = bpl.C_BPartner_Location_ID
	INNER JOIN M_Product p ON lus.M_product_ID = p.M_Product_ID
	LEFT OUTER JOIN M_Product_Trl pt ON p.M_product_ID = pt.M_Product_ID AND bp.AD_Language = pt.AD_Language
	LEFT OUTER JOIN C_BPartner_Product bpp ON bp.C_BPartner_ID = bpp.C_BPartner_ID AND p.M_Product_ID = bpp.M_Product_ID
	JOIN (
		SELECT 	lui.M_HU_ID, COALESCE(avg(tus.qty), 0) AS CU_per_TU, count(tu.M_HU_ID) AS TU_per_LU
		FROM 	M_HU_Item lui
			LEFT JOIN M_HU_PI_Item lupii ON lui.M_HU_PI_Item_ID = lupii.M_HU_PI_Item_ID AND lupii.ItemType = 'HU'
			LEFT JOIN M_HU tu ON lui.M_HU_Item_ID = tu.M_HU_Item_Parent_ID
			LEFT JOIN M_HU_Storage tus ON tu.M_HU_ID = tus.M_HU_ID
		GROUP BY lui.M_HU_ID
	) qty ON lu.M_HU_ID = qty.M_HU_ID

	LEFT OUTER JOIN M_HU_Attribute lua_sscc ON lu.M_HU_ID = lua_sscc.M_HU_ID
		AND lua_sscc.M_Attribute_ID = (SELECT M_Attribute_ID FROM M_Attribute WHERE name = 'SSCC18')
	LEFT OUTER JOIN M_HU_Attribute lua_netw ON lu.M_HU_ID = lua_netw.M_HU_ID
		AND lua_netw.M_Attribute_ID = (SELECT M_Attribute_ID FROM M_Attribute WHERE name = 'Gewicht Netto')
	LEFT OUTER JOIN M_HU_Attribute lua_grow ON lu.M_HU_ID = lua_grow.M_HU_ID
		AND lua_grow.M_Attribute_ID = (SELECT M_Attribute_ID FROM M_Attribute WHERE name = 'Gewicht Brutto kg')
	LEFT OUTER JOIN M_HU_Attribute lua_lot ON lu.M_HU_ID = lua_lot.M_HU_ID
		AND lua_lot.M_Attribute_ID = (SELECT M_Attribute_ID FROM M_Attribute WHERE name ILIKE 'Lot-Nummer')
-- WHERE
-- 	lu.M_HU_ID = $P{M_HU_ID}

;

ALTER VIEW report.fresh_HU_SSCC_Label_Report OWNER TO adempiere;














drop view if exists report.fresh_EDI_DesadvLine_SSCC_Label_Report;

create or replace view report.fresh_EDI_DesadvLine_SSCC_Label_Report
as
select
	/*
	 * NOTE to developer: please keep in sync with fresh_HU_SSCC_Label_Report
	 */
	 (
		SELECT COALESCE(org_bp.name, '') || ', ' || COALESCE(org_l.Postal, '')|| ' '  || COALESCE(org_l.city, '')
		FROM AD_Org org
		
		-- INFO I NEED
		INNER JOIN AD_OrgInfo org_info ON org.AD_Org_ID = org_info.AD_Org_ID
		INNER JOIN C_BPartner_Location org_bpl ON org_info.orgBP_Location_ID = org_bpl.C_BPartner_Location_ID
		INNER JOIN C_Location org_l ON org_bpl.C_Location_ID = org_l.C_Location_ID
		INNER JOIN c_bpartner org_bp ON org_bpl.C_Bpartner_ID = org_bp.C_BPartner_ID
		
		-- LINKING
				
		WHERE  org.ad_org_ID = o.ad_org_id
	 ) as org_address,
	dl_sscc.IPA_SSCC18 AS SSCC,
	dl.ProductNo AS p_CustomerValue,
	NULL::numeric AS PriceActual,
	COALESCE( pt.name, p.name ) AS p_name,
	dl_sscc.QtyCU ::numeric AS CU_per_TU,
	dl_sscc.QtyTU :: numeric AS TU_per_LU,
	NULL::numeric AS Net_Weight,
	NULL::numeric AS Gross_Weight,
	o.DocumentNo AS order_docno,
	p.value AS P_Value,
	NULL::varchar AS LotCode,
	sscc18_extract_serialNumber(dl_sscc.IPA_SSCC18) AS PaletNo,
	COALESCE(bp.name, '') || ' ' || COALESCE(hol.name, bpl.name, '') || E'\n' AS Customer,
	-- Adding language. This language is used for the captions in the report 
	bp.ad_Language,
	--
	-- Filtering fields
	NULL::numeric AS M_HU_ID,
	--
	dl_sscc.EDI_DesadvLine_SSCC_ID,
	sel.AD_PInstance_ID
--
from T_Selection sel
INNER JOIN EDI_DesadvLine_SSCC dl_sscc on (dl_sscc.EDI_DesadvLine_SSCC_ID=sel.T_Selection_ID)
INNER JOIN EDI_DesadvLine dl on (dl.EDI_DesadvLine_ID=dl_sscc.EDI_DesadvLine_ID)
INNER JOIN M_Product p on (p.M_Product_ID=dl.M_Product_ID)
LEFT OUTER JOIN C_OrderLine ol on (ol.EDI_DesadvLine_ID=dl.EDI_DesadvLine_ID)
LEFT OUTER JOIN C_Order o on (o.C_Order_ID=ol.C_Order_ID)
INNER JOIN EDI_Desadv dh on (dh.EDI_Desadv_ID=dl.EDI_Desadv_ID)
INNER JOIN C_BPartner bp ON (bp.C_BPartner_ID = dh.C_BPartner_ID)
LEFT OUTER JOIN C_BPartner_Location hol ON (hol.C_BPartner_Location_ID = dh.HandOver_Location_ID)
LEFT OUTER JOIN C_BPartner_Location bpl ON (bpl.C_BPartner_Location_ID = dh.C_BPartner_Location_ID)
LEFT OUTER JOIN M_Product_Trl pt ON (p.M_Product_ID = pt.M_Product_ID AND bp.AD_Language = pt.AD_Language)
;

ALTER VIEW report.fresh_EDI_DesadvLine_SSCC_Label_Report OWNER TO adempiere;