package de.metas.inoutcandidate.api.impl;

import java.util.List;

import org.adempiere.util.Check;
import org.junit.Ignore;

import de.metas.inoutcandidate.api.Packageable;
import de.metas.inoutcandidate.api.PackageableQuery;

@Ignore
public class MockedPackagingDAO extends PackagingDAO
{
	private List<Packageable> packageables;

	public void setPackableLines(final List<Packageable> packageables)
	{
		this.packageables = packageables;

	}

	@Override
	public List<Packageable> retrievePackableLines(final PackageableQuery query)
	{
		Check.assumeNotNull(packageables, "packageables were set before calling this method");
		return packageables;
	}
}
