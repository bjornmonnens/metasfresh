/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved. *
 * This program is free software; you can redistribute it and/or modify it *
 * under the terms version 2 of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. *
 * See the GNU General Public License for more details. *
 * You should have received a copy of the GNU General Public License along *
 * with this program; if not, write to the Free Software Foundation, Inc., *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA. *
 * For the text or an alternative of this public license, you may reach us *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA *
 * or via info@compiere.org or http://www.compiere.org/license.html *
 * Contributor(s): Carlos Ruiz - globalqss *
 * Teo Sarca - www.arhipac.ro *
 * Trifon Trifonov *
 *****************************************************************************/
package org.adempiere.ad.persistence.modelgen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.util.DisplayType;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

import de.metas.logging.LogManager;
import de.metas.util.Check;

/**
 * Generate Model Classes extending PO.
 * Base class for CMP interface - will be extended to create byte code directly
 *
 * @author Jorg Janke
 * @version $Id: GenerateModel.java,v 1.42 2005/05/08 15:16:56 jjanke Exp $
 *
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 *         <li>BF [ 1781629 ] Don't use Env.NL in model class/interface generators
 *         <li>FR [ 1781630 ] Generated class/interfaces have a lot of unused imports
 *         <li>BF [
 *         1781632 ] Generated class/interfaces should be UTF-8
 *         <li>FR [ xxxxxxx ] better formating of generated source
 *         <li>FR [ 1787876 ] ModelClassGenerator: list constants should be ordered
 *         <li>FR
 *         [ 1803309 ] Model generator: generate get method for Search cols
 *         <li>FR [ 1990848 ] Generated Models: remove hardcoded field length
 *         <li>FR [ 2343096 ] Model Generator: Improve Reference
 *         Class Detection
 *         <li>BF [ 2780468 ] ModelClassGenerator: not generating methods for Created*
 *         <li>--
 *         <li>FR [ 2848449 ] ModelClassGenerator: Implement model getters
 *         https://sourceforge.net/tracker/?func=detail&atid=879335&aid=2848449&group_id=176962
 * @author Victor Perez, e-Evolution
 *         <li>FR [ 1785001 ] Using ModelPackage of EntityType to Generate Model Class
 */
public class ModelClassGenerator
{
	private static final Logger log = LogManager.getLogger(ModelClassGenerator.class);
	private static final String NL = "\n";
	private static final String PLACEHOLDER_serialVersionUID = "[*serialVersionUID*]";
	private static final Set<String> COLUMNNAMES_STANDARD = ImmutableSet.of("AD_Client_ID", "AD_Org_ID", "IsActive", "Created", "CreatedBy", "Updated", "UpdatedBy");

	private final String packageName;

	public ModelClassGenerator(final TableInfo tableInfo, String directory, String packageName)
	{
		this.packageName = packageName;

		// create column access methods
		StringBuilder sb = createColumns(tableInfo);

		// Header
		String tableName = createHeader(tableInfo, sb, packageName);

		// Save
		if (!directory.endsWith(File.separator))
		{
			directory += File.separator;
		}

		writeToFile(sb, directory + tableName + ".java");
	}

	/**
	 * Add Header info to buffer
	 * 
	 * @param AD_Table_ID table
	 * @param sb buffer
	 * @param mandatory init call for mandatory columns
	 * @param packageName package name
	 * @return class name
	 */
	private String createHeader(final TableInfo tableInfo, StringBuilder sb, String packageName)
	{
		final String tableName = tableInfo.getTableName();

		//
		String keyColumn = tableName + "_ID";
		String className = "X_" + tableName;
		//
		StringBuilder start = new StringBuilder()
				.append(ModelInterfaceGenerator.COPY)
				.append("/** Generated Model - DO NOT CHANGE */").append(NL)
				.append("package " + packageName + ";").append(NL)
				.append(NL);

		addImportClass(java.util.Properties.class);
		addImportClass(java.sql.ResultSet.class);
		// if (!packageName.equals("org.compiere.model"))
		// addImportClass("org.compiere.model.*");
		createImports(start);
		// Class
		start.append("/** Generated Model for ").append(tableName).append(NL)
				.append(" *  @author metasfresh (generated) ").append(NL)
				.append(" */").append(NL)
				.append("@SuppressWarnings(\"javadoc\")").append(NL)
				.append("public class ").append(className)
				.append(" extends org.compiere.model.PO")
				.append(" implements I_").append(tableName)
				.append(", org.compiere.model.I_Persistent ")
				.append(NL)
				.append("{").append(NL)

				// serialVersionUID
				.append(NL)
				.append("\tprivate static final long serialVersionUID = ")
				.append(PLACEHOLDER_serialVersionUID) // generate serialVersionUID on save
				.append("L;").append(NL)

				// Standard Constructor
				.append(NL)
				.append("    /** Standard Constructor */").append(NL)
				.append("    public ").append(className).append(" (Properties ctx, int ").append(keyColumn).append(", String trxName)").append(NL)
				.append("    {").append(NL)
				.append("      super (ctx, ").append(keyColumn).append(", trxName);").append(NL)
				.append("    }").append(NL)
				// Constructor End

				// Load Constructor
				.append(NL)
				.append("    /** Load Constructor */").append(NL)
				.append("    public ").append(className).append(" (Properties ctx, ResultSet rs, String trxName)").append(NL)
				.append("    {").append(NL)
				.append("      super (ctx, rs, trxName);").append(NL)
				.append("    }").append(NL)
				// Load Constructor End

				// TableName
				// .append(NL)
				// .append(" /** TableName=").append(tableName).append(" */").append(NL)
				// .append(" public static final String Table_Name = \"").append(tableName).append("\";").append(NL)

				// AD_Table_ID
				// .append(NL)
				// .append(" /** AD_Table_ID=").append(AD_Table_ID).append(" */").append(NL)
				// .append(" public static final int Table_ID = MTable.getTable_ID(Table_Name);").append(NL)

				// KeyNamePair
				// .append(NL)
				// .append(" protected static KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);").append(NL)

				// accessLevel
				// .append(NL)
				// .append(" protected BigDecimal accessLevel = BigDecimal.valueOf(").append(accessLevel).append(");").append(NL)
				.append(NL);
		if (ModelInterfaceGenerator.isGenerateLegacy())
		{
			start
					.append("    /** AccessLevel").append(NL)
					.append("      * @return ").append(tableInfo.getAccessLevel().getDescription()).append(NL)
					.append("      */").append(NL)
					.append("    @Override").append(NL)
					.append("    protected int get_AccessLevel()").append(NL)
					.append("    {").append(NL)
					.append("      return accessLevel.intValue();").append(NL)
					.append("    }").append(NL);
		}

		// initPO
		start.append(NL)
				.append("\t/** Load Meta Data */").append(NL)
				.append("\t@Override").append(NL)
				.append("\tprotected org.compiere.model.POInfo initPO(Properties ctx)").append(NL)
				.append("\t{").append(NL)
				.append("\t\treturn org.compiere.model.POInfo.getPOInfo(Table_Name);").append(NL)
				.append("\t}").append(NL);

		StringBuilder end = new StringBuilder("}");
		//
		sb.insert(0, start);
		sb.append(end);

		return className;
	}

	/**
	 * Create Column access methods
	 * 
	 * @param AD_Table_ID table
	 * @param mandatory init call for mandatory columns
	 * @return set/get method
	 */
	private StringBuilder createColumns(final TableInfo tableInfo)
	{
		final StringBuilder sb = new StringBuilder();

		boolean isKeyNamePairCreated = false; // true if the method "getKeyNamePair" is already generated
		final List<ColumnInfo> columnInfos = tableInfo.getColumnInfos();
		for (final ColumnInfo columnInfo : columnInfos)
		{
			// Skip standard columns because for those we already have methods in org.compiere.model.PO
			if (COLUMNNAMES_STANDARD.contains(columnInfo.getColumnName()))
			{
				continue;
			}

			// Create record info KeyNamePair
			if (columnInfo.getSeqNo() == 1 && columnInfo.isIdentifier())
			{
				if (!isKeyNamePairCreated)
				{
					sb.append(createKeyNamePair(columnInfo.getColumnName(), columnInfo.getDisplayType()));
					isKeyNamePairCreated = true;
				}
				else
				{
					throw new RuntimeException("More than one primary identifier found: " + columnInfo);
				}
			}

			sb.append(createColumnMethods(columnInfo));
		}

		return sb;
	}	// createColumns

	/**
	 * Create set/get methods for column
	 * 
	 * @param mandatory init call for mandatory columns
	 * @param columnInfo
	 * @return set/get methods (java code)
	 */
	private String createColumnMethods(final ColumnInfo columnInfo)
	{
		final Class<?> clazz = ModelInterfaceGenerator.getClass(columnInfo);
		final int displayType = columnInfo.getDisplayType();
		final String dataType = ModelInterfaceGenerator.getDataTypeName(clazz, displayType);
		final String columnName = columnInfo.getColumnName();

		String defaultValue = columnInfo.getDefaultValue();
		if (defaultValue == null)
		{
			defaultValue = "";
		}

		// int fieldLength = columnInfo.getFieldLength();
		// if (DisplayType.isLOB(displayType)) // No length check for LOBs
		// fieldLength = 0;

		// Set ********
		String setValue = "\t\tset_Value";
		if (columnInfo.isEncrypted())
		{
			setValue = "\t\tset_ValueE";
		}
		// Handle isUpdateable
		if (!columnInfo.isUpdateable())
		{
			setValue = "\t\tset_ValueNoCheck";
			if (columnInfo.isEncrypted())
			{
				setValue = "\t\tset_ValueNoCheckE";
			}
		}

		StringBuilder sb = new StringBuilder();

		// TODO - New functionality
		// 1) Must understand which class to reference
		if (DisplayType.isID(displayType) && !columnInfo.isKey())
		{
			String fieldName = ModelInterfaceGenerator.getFieldName(columnName);
			String referenceClassName = ModelInterfaceGenerator.getReferenceClassName(columnInfo);
			//
			if (fieldName != null
					&& referenceClassName != null
					&& ModelInterfaceGenerator.isGenerateModelGetterOrSetterForReferencedClassName(referenceClassName))
			{
				//
				// Model Getter
				sb.append(NL)
						.append("\t@Override").append(NL)
						.append("\tpublic " + referenceClassName + " get").append(fieldName).append("()").append(NL)
						.append("\t{").append(NL)
						.append("\t\treturn get_ValueAsPO(COLUMNNAME_" + columnName + ", " + referenceClassName + ".class);").append(NL)
						.append("\t}").append(NL);

				//
				// Model Setter
				sb.append(NL)
						.append("\t@Override").append(NL)
						.append("\tpublic void set" + fieldName + "(" + referenceClassName + " " + fieldName + ")").append(NL)
						.append("\t{").append(NL)
						.append("\t\tset_ValueFromPO(COLUMNNAME_" + columnName + ", " + referenceClassName + ".class, " + fieldName + ");").append(NL)
						.append("\t}").append(NL);
			}
		}

		//
		// Handle AD_Table_ID/Record_ID generic model reference
		if (!Check.isEmpty(columnInfo.getTableIdColumnName(), true))
		{
			final String fieldName = ModelInterfaceGenerator.getFieldName(columnName);
			Check.assume("Record".equals(fieldName), "Generic reference field name shall be 'Record'");

			sb.append(NL)
					.append("\t@Override").append(NL)
					.append("\tpublic <RecordType> RecordType get").append(fieldName).append("(final Class<RecordType> recordType)").append(NL)
					.append("\t{").append(NL)
					.append("\t\t return getReferencedRecord(recordType);").append(NL)
					.append("\t}").append(NL);

			sb.append(NL)
					.append("\t@Override").append(NL)
					.append("\tpublic <RecordType> void set").append(fieldName).append("(final RecordType record)").append(NL)
					.append("\t{").append(NL)
					.append("\t\t setReferencedRecord(record);").append(NL)
					.append("\t}").append(NL);

			sb.append(NL);
		}

		//
		// Setter
		sb.append(NL);
		sb.append("\t@Override").append(NL);
		sb.append("\tpublic void set").append(columnName).append(" (").append(dataType).append(" ").append(columnName).append(")").append(NL)
				.append("\t{").append(NL);
		// List Validation
		if (columnInfo.getAdReferenceId() > 0 && String.class == clazz && columnInfo.getListInfo().isPresent())
		{
			sb.append("\n");

			final String staticVar = ADRefListGenerator.newInstance()
					.setColumnName(columnInfo.getColumnName())
					.setListInfo(columnInfo.getListInfo().get())
					.generateConstants();
			sb.insert(0, staticVar);
		}
		// setValue ("ColumnName", xx);
		if (columnInfo.isVirtualColumn())
		{
			sb.append("\t\tthrow new IllegalArgumentException (\"").append(columnName).append(" is virtual column\");");
		}
		// Integer
		else if (clazz.equals(Integer.class))
		{
			if (columnName.endsWith("_ID"))
			{
				final int firstValidId = InterfaceWrapperHelper.getFirstValidIdByColumnName(columnName);
				// set _ID to null if < 0 for special column or < 1 for others
				sb.append("\t\tif (").append(columnName).append(" < ").append(firstValidId).append(") ").append(NL)
						.append("\t").append(setValue).append(" (").append("COLUMNNAME_").append(columnName).append(", null);").append(NL)
						.append("\t\telse ").append(NL).append("\t");
			}
			sb.append(setValue).append(" (").append("COLUMNNAME_").append(columnName).append(", Integer.valueOf(").append(columnName).append("));").append(NL);
		}
		// Boolean
		else if (clazz.equals(Boolean.class))
		{
			sb.append(setValue).append(" (").append("COLUMNNAME_").append(columnName).append(", Boolean.valueOf(").append(columnName).append("));").append(NL);
		}
		else
		{
			sb.append(setValue).append(" (").append("COLUMNNAME_").append(columnName).append(", ")
					.append(columnName).append(");").append(NL);
		}
		sb.append("\t}").append(NL);

		//
		// Getter
		sb.append(NL);
		sb.append("\t@Override").append(NL);
		sb.append("\tpublic ").append(dataType);
		if (clazz.equals(Boolean.class))
		{
			sb.append(" is");
			if (columnName.toLowerCase().startsWith("is"))
			{
				sb.append(columnName.substring(2));
			}
			else
			{
				sb.append(columnName);
			}
		}
		else
		{
			sb.append(" get").append(columnName);
		}
		sb.append("() ").append(NL)
				.append("\t{").append(NL)
				.append("\t\t");
		if (clazz.equals(Integer.class))
		{
			sb.append("return get_ValueAsInt(").append("COLUMNNAME_").append(columnName).append(");").append(NL);
		}
		else if (clazz.equals(BigDecimal.class))
		{
			sb.append("BigDecimal bd = get_ValueAsBigDecimal(").append("COLUMNNAME_").append(columnName).append(");").append(NL);
			sb.append("\t\treturn bd != null ? bd : BigDecimal.ZERO;").append(NL);
			addImportClass(java.math.BigDecimal.class);
		}
		else if (clazz.equals(Boolean.class))
		{
			sb.append("return get_ValueAsBoolean(").append("COLUMNNAME_").append(columnName).append(");").append(NL);
		}
		else if (clazz.equals(java.sql.Timestamp.class))
		{
			sb.append("return get_ValueAsTimestamp(").append("COLUMNNAME_").append(columnName).append(");").append(NL);
		}
		else if (dataType.equals("Object"))
		{
			final String getValue = columnInfo.isEncrypted() ? "get_ValueE" : "get_Value";

			sb.append("\t\treturn ").append(getValue)
					.append("(").append("COLUMNNAME_").append(columnName).append(");").append(NL);
		}
		else
		{
			final String getValue = columnInfo.isEncrypted() ? "get_ValueE" : "get_Value";

			sb.append("return (").append(dataType).append(")").append(getValue)
					.append("(").append("COLUMNNAME_").append(columnName).append(");").append(NL);
			// addImportClass(clazz);
		}
		sb.append("\t}").append(NL);
		//
		return sb.toString();
	}	// createColumnMethods

	/**
	 * Create getKeyNamePair() method with first identifier
	 *
	 * @param columnName name
	 *            * @param displayType int
	 * @return method code
	 */
	private StringBuilder createKeyNamePair(String columnName, int displayType)
	{
		if (!ModelInterfaceGenerator.isGenerateLegacy())
		{
			return new StringBuilder();
		}

		String method = "get" + columnName + "()";
		if (displayType != DisplayType.String)
		{
			method = "String.valueOf(" + method + ")";
		}

		StringBuilder sb = new StringBuilder(NL)
				.append("    /** Get Record ID/ColumnName").append(NL)
				.append("        @return ID/ColumnName pair").append(NL)
				.append("      */").append(NL)
				.append("    public org.compiere.util.KeyNamePair getKeyNamePair() ").append(NL)
				.append("    {").append(NL)
				.append("        return new org.compiere.util.KeyNamePair(get_ID(), ").append(method).append(");").append(NL)
				.append("    }").append(NL);
		// addImportClass(org.compiere.util.KeyNamePair.class);
		return sb;
	}	// createKeyNamePair

	/**************************************************************************
	 * Write to file
	 * 
	 * @param sb string buffer
	 * @param fileName file name
	 */
	private void writeToFile(StringBuilder sb, String fileName)
	{
		// Generate serial number
		{
			String s = sb.toString();
			int hash = s.hashCode();
			s = s.replace(PLACEHOLDER_serialVersionUID, String.valueOf(hash));
			sb = new StringBuilder(s);
			System.out.println("" + fileName + ": hash=" + hash);
		}

		try
		{
			File out = new File(fileName);
			Writer fw = new OutputStreamWriter(new FileOutputStream(out, false), "UTF-8");
			for (int i = 0; i < sb.length(); i++)
			{
				char c = sb.charAt(i);
				// after
				if (c == ';' || c == '}')
				{
					fw.write(c);
					if (sb.substring(i + 1).startsWith("//"))
					{
						// fw.write('\t');
					}
					else
					{
						// fw.write(NL);
					}
				}
				// before & after
				else if (c == '{')
				{
					// fw.write(NL);
					fw.write(c);
					// fw.write(NL);
				}
				else
				{
					fw.write(c);
				}
			}
			fw.flush();
			fw.close();
			float size = out.length();
			size /= 1024;
			log.info(out.getAbsolutePath() + " - " + size + " kB");
		}
		catch (Exception ex)
		{
			log.error(fileName, ex);
			throw new RuntimeException(ex);
		}
	}

	/** Import classes */
	private Collection<String> s_importClasses = new TreeSet<>();

	/**
	 * Add class name to class import list
	 * 
	 * @param className
	 */
	private void addImportClass(String className)
	{
		if (className == null
				|| (className.startsWith("java.lang.") && !className.startsWith("java.lang.reflect."))
				|| className.startsWith(packageName + "."))
		{
			return;
		}
		for (String name : s_importClasses)
		{
			if (className.equals(name))
			{
				return;
			}
		}
		s_importClasses.add(className);
	}

	/**
	 * Add class to class import list
	 * 
	 * @param cl
	 */
	private void addImportClass(Class<?> cl)
	{
		if (cl.isArray())
		{
			cl = cl.getComponentType();
		}
		if (cl.isPrimitive())
		{
			return;
		}
		addImportClass(cl.getCanonicalName());
	}

	/**
	 * Generate java imports
	 * 
	 * @param sb
	 */
	private void createImports(StringBuilder sb)
	{
		for (String name : s_importClasses)
		{
			sb.append("import ").append(name).append(";").append(NL);
		}
		sb.append(NL);
	}
}
