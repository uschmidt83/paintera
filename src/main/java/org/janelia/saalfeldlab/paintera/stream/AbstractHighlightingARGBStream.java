/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.saalfeldlab.paintera.stream;

import java.lang.invoke.MethodHandles;

import org.janelia.saalfeldlab.fx.ObservableWithListenersList;
import org.janelia.saalfeldlab.paintera.control.assignment.FragmentSegmentAssignment;
import org.janelia.saalfeldlab.paintera.control.assignment.FragmentSegmentAssignmentState;
import org.janelia.saalfeldlab.paintera.control.selection.SelectedIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TLongIntHashMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import net.imglib2.type.label.Label;

/**
 * Generates and caches a stream of colors.
 *
 * @author Stephan Saalfeld
 * @author Philipp Hanslovsky
 *
 */
public abstract class AbstractHighlightingARGBStream extends ObservableWithListenersList implements ARGBStream
{

	public static int DEFAULT_ALPHA= 0x20000000;

	public static int DEFAULT_ACTIVE_FRAGMENT_ALPHA= 0xd0000000;

	public static int DEFAULT_ACTIVE_SEGMENT_ALPHA= 0x80000000;

	private static final Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	final static protected double[] rs = new double[] { 1, 1, 0, 0, 0, 1, 1 };

	final static protected double[] gs = new double[] { 0, 1, 1, 1, 0, 0, 0 };

	final static protected double[] bs = new double[] { 0, 0, 0, 1, 1, 1, 0 };

	private static final int ZERO = 0x00000000;

	protected long seed = 0;

	protected int alpha = DEFAULT_ALPHA;

	protected int activeFragmentAlpha = DEFAULT_ACTIVE_FRAGMENT_ALPHA;

	protected int activeSegmentAlpha = DEFAULT_ACTIVE_SEGMENT_ALPHA;

	protected int invalidSegmentAlpha = ZERO;

	protected SelectedIds highlights;

	protected FragmentSegmentAssignment assignment;

	private final BooleanProperty colorFromSegmentId = new SimpleBooleanProperty();

	public AbstractHighlightingARGBStream( final SelectedIds highlights, final FragmentSegmentAssignmentState assignment )
	{
		this.highlights = highlights;
		this.assignment = assignment;
		this.colorFromSegmentId.addListener( ( obs, oldv, newv ) -> stateChanged() );
	}

	protected TLongIntHashMap argbCache = new TLongIntHashMap(
			Constants.DEFAULT_CAPACITY,
			Constants.DEFAULT_LOAD_FACTOR,
			Label.TRANSPARENT,
			0 );

//	public void highlight( final TLongHashSet highlights )
//	{
//		this.highlights.clear();
//		this.highlights.addAll( highlights );
//	}
//
//	public void highlight( final long[] highlights )
//	{
//		this.highlights.clear();
//		this.highlights.addAll( highlights );
//	}

	public boolean isActiveFragment( final long id )
	{
		// TODO FIX THIS THING HERE!
		for ( final long i : highlights.getActiveIds() )
		{
			if ( id == i ) { return true; }
		}

		return false;
	}

	public boolean isActiveSegment( final long id )
	{
		// TODO FIX THIS THING HERE!
		final long segment = this.assignment.getSegment( id );
		for ( final long i : highlights.getActiveIds() )
		{
			if ( this.assignment.getSegment( i ) == segment ) { return true; }
		}
		return false;
	}

	final static protected int argb( final int r, final int g, final int b, final int alpha )
	{
		return ( r << 8 | g ) << 8 | b | alpha;
	}

	protected double getDouble( final long id )
	{
		return getDoubleImpl( id, colorFromSegmentId.get() );
	}

	protected abstract double getDoubleImpl( final long id, boolean colorFromSegmentId );

	@Override
	public int argb( final long id )
	{
		return id == Label.TRANSPARENT ? ZERO : argbImpl( id, colorFromSegmentId.get() );
	}

	protected abstract int argbImpl( long id, boolean colorFromSegmentId );

	/**
	 * Change the seed.
	 *
	 * @param seed
	 */
	public void setSeed( final long seed )
	{
		if ( this.seed != seed )
		{
			this.seed = seed;
			stateChanged();
		}
	}

	/**
	 * Increment seed.
	 */
	public void incSeed()
	{
		setSeed( seed + 1 );
	}

	/**
	 * Decrement seed.
	 */
	public void decSeed()
	{
		setSeed( seed - 1 );
	}

	/**
	 *
	 * @return The current seed value
	 */
	public long getSeed()
	{
		return this.seed;
	}

	/**
	 * Change alpha. Values less or greater than [0,255] will be masked.
	 *
	 * @param alpha
	 */
	public void setAlpha( final int alpha )
	{
		if ( getAlpha() != alpha )
		{
			this.alpha = ( alpha & 0xff ) << 24;
			stateChanged();
		}
	}

	/**
	 * Change active fragment alpha. Values less or greater than [0,255] will be
	 * masked.
	 *
	 * @param alpha
	 */
	public void setActiveSegmentAlpha( final int alpha )
	{
		if ( getActiveSegmentAlpha() != alpha )
		{
			this.activeSegmentAlpha = ( alpha & 0xff ) << 24;
			stateChanged();
		}
	}

	public void setInvalidSegmentAlpha( final int alpha )
	{
		if ( getInvalidSegmentAlpha() != alpha )
		{
			this.invalidSegmentAlpha = ( alpha & 0xff ) << 24;
			stateChanged();
		}
	}

	public void setActiveFragmentAlpha( final int alpha )
	{
		if ( getActiveFragmentAlpha() != alpha )
		{
			this.activeFragmentAlpha = ( alpha & 0xff ) << 24;
			stateChanged();
		}
	}

	public int getAlpha()
	{
		return this.alpha >>> 24;
	}

	public int getActiveSegmentAlpha()
	{
		return this.activeSegmentAlpha >>> 24;
	}

	public int getInvalidSegmentAlpha()
	{
		return invalidSegmentAlpha >>> 24;
	}

	public int getActiveFragmentAlpha()
	{
		return this.activeFragmentAlpha >>> 24;
	}

	public void clearCache()
	{
		LOG.debug( "Before clearing cache: {}", argbCache );
		argbCache.clear();
		LOG.debug( "After clearing cache: {}", argbCache );
		stateChanged();
	}

	public void setColorFromSegmentId( final boolean fromSegmentId )
	{
		this.colorFromSegmentId.set( fromSegmentId );
	}

	public boolean getColorFromSegmentId()
	{
		return this.colorFromSegmentId.get();
	}

	public BooleanProperty colorFromSegmentIdProperty()
	{
		return this.colorFromSegmentId;
	}

	public void setHighlights( final SelectedIds highlights )
	{
		this.highlights = highlights;
		clearCache();
	}

	public void setAssignment( final FragmentSegmentAssignment assignment )
	{
		this.assignment = assignment;
		clearCache();
	}

	public void setHighlightsAndAssignment(
			final SelectedIds highlights,
			final FragmentSegmentAssignment assignment )
	{
		this.highlights = highlights;
		this.assignment = assignment;
		clearCache();
	}

}
