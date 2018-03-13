package net.imglib2.img.basictypeaccess.delegate.dirty;

import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileShortAccess;

public class DirtyVolatileDelegateShortAccess extends DirtyDelegateShortAccess implements VolatileShortAccess
{

	private final boolean isValid;

	public DirtyVolatileDelegateShortAccess( final ShortAccess access, final boolean isValid )
	{
		super( access );
		this.isValid = isValid;
	}

	@Override
	public boolean isValid()
	{
		return this.isValid;
	}

}
