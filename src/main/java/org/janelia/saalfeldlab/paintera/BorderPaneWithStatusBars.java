package org.janelia.saalfeldlab.paintera;

import bdv.fx.viewer.ViewerPanelFX;
import bdv.viewer.Source;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import net.imglib2.RealPoint;
import org.janelia.saalfeldlab.fx.TitledPanes;
import org.janelia.saalfeldlab.fx.ortho.OrthogonalViews;
import org.janelia.saalfeldlab.fx.ortho.OrthogonalViews.ViewerAndTransforms;
import org.janelia.saalfeldlab.fx.ui.NumberField;
import org.janelia.saalfeldlab.fx.ui.ObjectField;
import org.janelia.saalfeldlab.fx.ui.ResizeOnLeftSide;
import org.janelia.saalfeldlab.fx.ui.SingleChildStackPane;
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread;
import org.janelia.saalfeldlab.paintera.cache.MemoryBoundedSoftRefLoaderCache;
import org.janelia.saalfeldlab.paintera.config.ArbitraryMeshConfig;
import org.janelia.saalfeldlab.paintera.config.ArbitraryMeshConfigNode;
import org.janelia.saalfeldlab.paintera.config.BookmarkConfigNode;
import org.janelia.saalfeldlab.paintera.config.CrosshairConfigNode;
import org.janelia.saalfeldlab.paintera.config.NavigationConfigNode;
import org.janelia.saalfeldlab.paintera.config.OrthoSliceConfigNode;
import org.janelia.saalfeldlab.paintera.config.ScaleBarOverlayConfigNode;
import org.janelia.saalfeldlab.paintera.config.ScreenScalesConfigNode;
import org.janelia.saalfeldlab.paintera.config.Viewer3DConfigNode;
import org.janelia.saalfeldlab.paintera.control.navigation.CoordinateDisplayListener;
import org.janelia.saalfeldlab.paintera.state.SourceInfo;
import org.janelia.saalfeldlab.paintera.ui.Crosshair;
import org.janelia.saalfeldlab.paintera.ui.source.SourceTabs;
import org.janelia.saalfeldlab.paintera.viewer3d.OrthoSliceFX;
import org.janelia.saalfeldlab.util.Colors;
import org.janelia.saalfeldlab.util.MakeUnchecked;
import org.janelia.saalfeldlab.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

public class BorderPaneWithStatusBars
{

	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final BorderPane pane;

	private final HBox statusBar;

	private final ScrollPane sideBar;

	private final Label currentSourceStatus;

	private final Label viewerCoordinateStatus;

	private final Label worldCoordinateStatus;

	private final Label valueStatus;

	private final ResizeOnLeftSide resizeSideBar;

	private final NavigationConfigNode navigationConfigNode = new NavigationConfigNode();

	private final CrosshairConfigNode crosshairConfigNode = new CrosshairConfigNode();

	private final OrthoSliceConfigNode orthoSliceConfigNode = new OrthoSliceConfigNode();

	private final Viewer3DConfigNode viewer3DConfigNode = new Viewer3DConfigNode();

	private final ScreenScalesConfigNode screenScaleConfigNode = new ScreenScalesConfigNode();

	private final ScaleBarOverlayConfigNode scaleBarConfigNode = new ScaleBarOverlayConfigNode();

	private final BookmarkConfigNode bookmarkConfigNode;

	private final ArbitraryMeshConfigNode arbitraryMeshConfigNode = new ArbitraryMeshConfigNode();

	private final Map<ViewerAndTransforms, Crosshair> crossHairs;

	private final Map<ViewerAndTransforms, OrthoSliceFX> orthoSlices;

	private final ObservableObjectValue<ViewerAndTransforms> currentFocusHolderWithState;

	private final Button saveProjectButton;

	public final ObservableObjectValue<ViewerAndTransforms> currentFocusHolder()
	{
		return this.currentFocusHolderWithState;
	}

	public BorderPane getPane()
	{
		return this.pane;
	}

	public void setViewerCoordinateStatus(final RealPoint p)
	{
		InvokeOnJavaFXApplicationThread.invoke(() -> viewerCoordinateStatus.setText(p == null
		                                                                            ? "N/A"
		                                                                            : String.format("(% 4d, % 4d)",
				                                                                            (int) p.getDoublePosition
						                                                                            (0),
				                                                                            (int) p
						                                                                            .getDoublePosition(1)
		                                                                                           )));
	}

	public void setWorldCoorinateStatus(final RealPoint p)
	{
		InvokeOnJavaFXApplicationThread.invoke(() -> worldCoordinateStatus.setText(p == null
		                                                                           ? "N/A"
		                                                                           : CoordinateDisplayListener
				                                                                           .worldToString(
				                                                                           p)));
	}

	public void setCurrentValue(final String s)
	{
		InvokeOnJavaFXApplicationThread.invoke(() -> valueStatus.setText(s));
	}

	public Map<ViewerAndTransforms, OrthoSliceFX> orthoSlices()
	{
		return Collections.unmodifiableMap(this.orthoSlices);
	}

	public BorderPaneWithStatusBars(
			final PainteraBaseView center,
			final Supplier<String> project)
	{
		LOG.debug("Construction {}", BorderPaneWithStatusBars.class.getName());
		this.pane = new BorderPane(center.orthogonalViews().pane());

		this.currentFocusHolderWithState = currentFocusHolder(center.orthogonalViews());

		this.currentSourceStatus = new Label();
		this.viewerCoordinateStatus = new Label();
		this.worldCoordinateStatus = new Label();
		this.valueStatus = new Label();
		final CheckBox showStatusBar = new CheckBox();
		showStatusBar.setFocusTraversable(false);
		showStatusBar.setTooltip(new Tooltip("If not selected, status bar will only show on mouse-over"));

		this.bookmarkConfigNode =  new BookmarkConfigNode(bm -> {
			center.manager().setTransform(bm.getGlobalTransformCopy());
			center.viewer3D().setAffine(bm.getViewer3DTransformCopy());
		});

		this.crossHairs = makeCrosshairs(center.orthogonalViews(), Colors.CREMI, Color.WHITE.deriveColor(0, 1, 1,
				0.5));
		this.orthoSlices = makeOrthoSlices(
				center.orthogonalViews(),
				center.viewer3D().meshesGroup(),
				center.sourceInfo()
			);

		final SingleChildStackPane sourceDisplayStatus = new SingleChildStackPane();
		center.sourceInfo().currentState().addListener((obs, oldv, newv) -> sourceDisplayStatus.setChild(newv.getDisplayStatus()));

		// show source name by default, or override it with source status text if any
		center.sourceInfo().currentState().addListener((obs, oldv, newv) -> {
			sourceDisplayStatus.setChild(newv.getDisplayStatus());
			currentSourceStatus.textProperty().unbind();
			currentSourceStatus.textProperty().bind(Bindings.createStringBinding(
					() -> {
						if (newv.statusTextProperty() != null && newv.statusTextProperty().get() != null)
							return newv.statusTextProperty().get();
						else if (newv.nameProperty().get() != null)
							return newv.nameProperty().get();
						else
							return null;
					},
					newv.nameProperty(),
					newv.statusTextProperty()
				));
		});

		// for positioning the 'show status bar' checkbox on the right
		final Region valueStatusSpacing = new Region();
		HBox.setHgrow(valueStatusSpacing, Priority.ALWAYS);

		this.statusBar = new HBox(5,
				sourceDisplayStatus,
				currentSourceStatus,
				viewerCoordinateStatus,
				worldCoordinateStatus,
				valueStatus,
				valueStatusSpacing,
				showStatusBar
		);

		final Tooltip currentSourceStatusToolTip = new Tooltip();
		currentSourceStatusToolTip.textProperty().bind(currentSourceStatus.textProperty());
		currentSourceStatus.setTooltip(currentSourceStatusToolTip);

		currentSourceStatus.setPrefWidth(95.0);
		viewerCoordinateStatus.setPrefWidth(115.0);
		worldCoordinateStatus.setPrefWidth(245.0);

		viewerCoordinateStatus.setFont(Font.font("Monospaced"));
		worldCoordinateStatus.setFont(Font.font("Monospaced"));

		final BooleanProperty isWithinMarginOfBorder = new SimpleBooleanProperty();
		pane.addEventFilter(
				MouseEvent.MOUSE_MOVED,
				e -> isWithinMarginOfBorder.set(e.getY() < pane.getHeight() && pane.getHeight() - e.getY() <=
						statusBar.getHeight())
		                   );
		statusBar.visibleProperty().addListener((obs, oldv, newv) -> pane.setBottom(newv ? statusBar : null));
		statusBar.visibleProperty().bind(isWithinMarginOfBorder.or(showStatusBar.selectedProperty()));
		showStatusBar.setSelected(true);

		final BiConsumer<Source<?>, Exception> onRemoveException = (s, e) -> {
			LOG.warn("Unable to remove source: {}", e.getMessage());
		};

		final SourceTabs sourceTabs = new SourceTabs(
				center.sourceInfo().currentSourceIndexProperty(),
				MakeUnchecked.onException(center.sourceInfo()::removeSource, onRemoveException),
				center.sourceInfo()
		);

		final TitledPane sourcesContents = new TitledPane("sources", sourceTabs.get());
		sourcesContents.setExpanded(false);

		LongUnaryOperator toMegaBytes = bytes -> bytes / 1000 / 1000;
		LongSupplier currentMemory = center::getCurrentMemoryUsageInBytes;
		LongSupplier maxMemory = ((MemoryBoundedSoftRefLoaderCache<?, ?, ?>)center.getGlobalBackingCache())::getMaxSize;
		Supplier<String> currentMemoryStr = () -> Long.toString(toMegaBytes.applyAsLong(currentMemory.getAsLong()));
		Supplier<String> maxMemoryStr = () -> Long.toString(toMegaBytes.applyAsLong(maxMemory.getAsLong()));
		final Label memoryUsageField = new Label(String.format("%s/%s", currentMemoryStr.get(), maxMemoryStr.get()));
		final Timeline currentMemoryUsageUPdateTask = new Timeline(new KeyFrame(
				Duration.seconds(1),
				e -> memoryUsageField.setText(String.format("%s/%s", currentMemoryStr.get(), maxMemoryStr.get()))));
		currentMemoryUsageUPdateTask.setCycleCount(Timeline.INDEFINITE);
		currentMemoryUsageUPdateTask.play();

		// TODO put this stuff in a better place!
		final ScheduledExecutorService memoryCleanupScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("cache clean up", true));
		memoryCleanupScheduler.scheduleAtFixedRate(((MemoryBoundedSoftRefLoaderCache<?, ?, ?>)center.getGlobalBackingCache())::restrictToMaxSize,0, 3, TimeUnit.SECONDS);

		Button setButton = new Button("Set");
		setButton.setOnAction(e -> {
			Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
			NumberField<LongProperty> field = NumberField.longField(
					maxMemory.getAsLong(),
					val -> val > 0 && val < Runtime.getRuntime().maxMemory(),
					ObjectField.SubmitOn.ENTER_PRESSED,
					ObjectField.SubmitOn.FOCUS_LOST);
			dialog.getDialogPane().setContent(field.textField());
			if (ButtonType.OK.equals(dialog.showAndWait().orElse(ButtonType.CANCEL)))
			{
				new Thread(() -> {
					((MemoryBoundedSoftRefLoaderCache<?, ?, ?>)center.getGlobalBackingCache()).setMaxSize(field.valueProperty().get());
					InvokeOnJavaFXApplicationThread.invoke(() -> memoryUsageField.setText(String.format("%s/%s", currentMemoryStr.get(), maxMemoryStr.get())));
				}).start();
			}
		});


		final TitledPane memoryUsage = TitledPanes.createCollapsed("Memory", new HBox(new Label("Cache Size"), memoryUsageField, setButton));

		final VBox settingsContents = new VBox(
				this.navigationConfigNode.getContents(),
				this.crosshairConfigNode.getContents(),
				this.orthoSliceConfigNode.getContents(),
				this.viewer3DConfigNode.getContents(),
				this.scaleBarConfigNode,
				this.bookmarkConfigNode,
				this.arbitraryMeshConfigNode,
				this.screenScaleConfigNode.getContents(),
				memoryUsage
		);
		final TitledPane settings = new TitledPane("settings", settingsContents);
		settings.setExpanded(false);

		center.viewer3D().meshesGroup().getChildren().add(this.arbitraryMeshConfigNode.getMeshGroup());

		saveProjectButton = new Button("Save");

		this.sideBar = new ScrollPane(new VBox(sourcesContents, settings, saveProjectButton));
		this.sideBar.setHbarPolicy(ScrollBarPolicy.NEVER);
		this.sideBar.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		this.sideBar.setVisible(true);
		this.sideBar.prefWidthProperty().set(280);
		sourceTabs.widthProperty().bind(sideBar.prefWidthProperty());
		settingsContents.prefWidthProperty().bind(sideBar.prefWidthProperty());

		resizeSideBar = new ResizeOnLeftSide(sideBar, sideBar.prefWidthProperty(), dist -> Math.abs(dist) < 5);
	}

	public ScrollPane getSideBar()
	{
		return sideBar;
	}

	public void toggleSideBar()
	{
		if (pane.getRight() == null)
		{
			pane.setRight(sideBar);
			resizeSideBar.install();
		}
		else
		{
			resizeSideBar.remove();
			pane.setRight(null);
		}
	}

	public ObjectProperty<EventHandler<ActionEvent>> saveProjectButtonOnActionProperty()
	{
		return this.saveProjectButton.onActionProperty();
	}

	public static Map<ViewerAndTransforms, Crosshair> makeCrosshairs(
			final OrthogonalViews<?> views,
			final Color onFocusColor,
			final Color offFocusColor)
	{
		final Map<ViewerAndTransforms, Crosshair> map = new HashMap<>();
		map.put(views.topLeft(), makeCrossHairForViewer(views.topLeft().viewer(), onFocusColor, offFocusColor));
		map.put(views.topRight(), makeCrossHairForViewer(views.topRight().viewer(), onFocusColor, offFocusColor));
		map.put(views.bottomLeft(), makeCrossHairForViewer(views.bottomLeft().viewer(), onFocusColor, offFocusColor));
		return map;
	}

	public static Crosshair makeCrossHairForViewer(
			final ViewerPanelFX viewer,
			final Color onFocusColor,
			final Color offFocusColor)
	{
		final Crosshair ch = new Crosshair();
		viewer.getDisplay().addOverlayRenderer(ch);
		ch.wasChangedProperty().addListener((obs, oldv, newv) -> viewer.getDisplay().drawOverlays());
		ch.isHighlightProperty().bind(viewer.focusedProperty());
		return ch;
	}

	public static Map<ViewerAndTransforms, OrthoSliceFX> makeOrthoSlices(
			final OrthogonalViews<?> views,
			final Group scene,
			final SourceInfo sourceInfo)
	{
		final Map<ViewerAndTransforms, OrthoSliceFX> map = new HashMap<>();
		map.put(views.topLeft(), new OrthoSliceFX(scene, views.topLeft().viewer()));
		map.put(views.topRight(), new OrthoSliceFX(scene, views.topRight().viewer()));
		map.put(views.bottomLeft(), new OrthoSliceFX(scene, views.bottomLeft().viewer()));
		return map;
	}

	public static ObservableObjectValue<ViewerAndTransforms> currentFocusHolder(final OrthogonalViews<?> views)
	{
		final ViewerAndTransforms     tl      = views.topLeft();
		final ViewerAndTransforms     tr      = views.topRight();
		final ViewerAndTransforms     bl      = views.bottomLeft();
		final ReadOnlyBooleanProperty focusTL = tl.viewer().focusedProperty();
		final ReadOnlyBooleanProperty focusTR = tr.viewer().focusedProperty();
		final ReadOnlyBooleanProperty focusBL = bl.viewer().focusedProperty();

		return Bindings.createObjectBinding(
				() -> {
					return focusTL.get() ? tl : focusTR.get() ? tr : focusBL.get() ? bl : null;
				},
				focusTL,
				focusTR,
				focusBL
		                                   );

	}

	public NavigationConfigNode navigationConfigNode()
	{
		return this.navigationConfigNode;
	}

	public CrosshairConfigNode crosshairConfigNode()
	{
		return this.crosshairConfigNode;
	}

	public OrthoSliceConfigNode orthoSliceConfigNode()
	{
		return this.orthoSliceConfigNode;
	}

	public ScreenScalesConfigNode screenScalesConfigNode() { return this.screenScaleConfigNode; }

	public Viewer3DConfigNode viewer3DConfigNode()
	{
		return this.viewer3DConfigNode;
	}

	public ScaleBarOverlayConfigNode scaleBarOverlayConfigNode() {
		return this.scaleBarConfigNode;
	}

	public BookmarkConfigNode bookmarkConfigNode() {
		return this.bookmarkConfigNode;
	}

	public ArbitraryMeshConfigNode arbitraryMeshConfigNode() {
		return this.arbitraryMeshConfigNode;
	}

	public Map<ViewerAndTransforms, Crosshair> crosshairs()
	{
		return Collections.unmodifiableMap(crossHairs);
	}
}
