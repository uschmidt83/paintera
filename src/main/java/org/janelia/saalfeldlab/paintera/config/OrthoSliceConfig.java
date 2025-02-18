package org.janelia.saalfeldlab.paintera.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import org.janelia.saalfeldlab.paintera.viewer3d.OrthoSliceFX;

public class OrthoSliceConfig
{

	private final OrthoSliceConfigBase baseConfig;

	final ObservableBooleanValue isTopLeftVisible;

	final ObservableBooleanValue isTopRightVisible;

	final ObservableBooleanValue isBottomLeftVisible;

	private final ObservableBooleanValue hasSources;

	public OrthoSliceConfig(
			final OrthoSliceConfigBase baseConfig,
			final ObservableBooleanValue isTopLeftVisible,
			final ObservableBooleanValue isTopRightVisible,
			final ObservableBooleanValue isBottomLeftVisible,
			final ObservableBooleanValue hasSources)
	{
		super();
		this.baseConfig = baseConfig;
		this.isTopLeftVisible = isTopLeftVisible;
		this.isTopRightVisible = isTopRightVisible;
		this.isBottomLeftVisible = isBottomLeftVisible;
		this.hasSources = hasSources;
	}

	public BooleanProperty enableProperty()
	{
		return this.baseConfig.isEnabledProperty();
	}

	public BooleanProperty showTopLeftProperty()
	{
		return this.baseConfig.showTopLeftProperty();
	}

	public BooleanProperty showTopRightProperty()
	{
		return this.baseConfig.showTopRightProperty();
	}

	public BooleanProperty showBottomLeftProperty()
	{
		return this.baseConfig.showBottomLeftProperty();
	}

	public void bindOrthoSlicesToConfig(
			final OrthoSliceFX topLeft,
			final OrthoSliceFX topRight,
			final OrthoSliceFX bottomLeft)
	{
		final BooleanProperty enable = baseConfig.isEnabledProperty();
		topLeft.isVisibleProperty().bind(baseConfig.showTopLeftProperty().and(enable).and(hasSources).and(
				isTopLeftVisible));
		topRight.isVisibleProperty().bind(baseConfig.showTopRightProperty().and(enable).and(hasSources).and(
				isTopRightVisible));
		bottomLeft.isVisibleProperty().bind(baseConfig.showBottomLeftProperty().and(enable).and(hasSources).and(
				isBottomLeftVisible));
	}
}
