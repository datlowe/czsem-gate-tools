package czsem.netgraph.batik;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGLocatable;
import org.w3c.dom.svg.SVGRect;

import czsem.netgraph.TreeComputation;
import czsem.netgraph.treesource.TreeSourceWithSelectionSupport;
import czsem.netgraph.treesource.TreeSource.NodeLabel;

public class BatikTreeBuilder<E> {
	
	public static final class NodeType {
		public static final int STANDARD = 0; 
		public static final int EMPHASIZED = 1; 
		public static final int OPTIONAL = 2; 
		public static final int WARN = 3; 
	}
	
	public static final class Sizing {
		public static final int NODE_DIAM = 9; 

		public static final int BORDER = 8;
		public static final float NODE_V_SPACE = (int)(NODE_DIAM*1.3);
		public static final int NODE_H_SPACE = NODE_DIAM;
		public static final int TEXT_H_SPACE = NODE_DIAM;

		public static final int LINE_HEIGHT = 15; 
		public static final int FIRST_LINE_Y = (int)(NODE_DIAM*2.0);
		
		public static final String FONT_SIZE = "14";
		public static final String FONT_STROKE = "5";
		public static final float TEXT_OFFSET_MIDDLE = 1.5f;

		public static final String EDGE_STROKE = "2.6";
		public static final float LINK_STROKE_F = 1.6f;
		public static final String LINK_STROKE = ((Float)LINK_STROKE_F).toString();
		public static final String LINK_DASHARRAY = "5,1";		

		public static final String NODE_STROKE = "1.5";
		public static final String NODE_STROKE_SELECTED = "3";

		public static final float LINK_END_SHIFT = NODE_DIAM*0.9f;

		public static final float LINK_SELF_RADIUS = 10f;
	}
	
	public static final class Color {
		public static final String TEXT_STROKE = "white";
		public static final String TEXT_STROKE_OPACITY = "0.9";
		public static final String TEXT = "#333";
		public static final String NODE_STROKE = "#666666";
		public static final String FRAME_FILL = "white";
		public static final String FRAME_OPACITY = "0";
		public static final java.awt.Color CANVAS_BACKGROUND = java.awt.Color.WHITE;
		public static final String NODE_FILL_SELECTED = "#EFC94C";
		public static final String NODE_STROKE_SELECTED = "#c7001e";
		
		public static final String[] NODE = {
			"#669eff", //STANDARD = 0; 
			"#91d4c7",//EMPHASIZED = 1; 
			"#b3b3b3",//OPTIONAL = 2; 
			"#ea7b8b", //WARN = 3; 
		};

		public static final String[] EDGE = {
			"#336699",//STANDARD = 0; 
			"#399381",//EMPHASIZED = 1; 
			NODE_STROKE,//OPTIONAL = 2; 
			"#b01c32", //WARN = 3; 
		};
	}
	
	public static final String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;

	protected final TreeSourceWithSelectionSupport<E> treeSource;
	protected TreeComputation<E> cmp;

	protected SVGDocument doc;
	protected Element svgRoot;
	protected Element mainGroupRoot;
	protected Element edgesGroup;
		
	protected float[] x;
	protected float[] y;
	
	protected E[] srcNodes;
	protected Element[] svgNodes;
	protected Integer[] sortedNodes;

	protected int[] edges;
	protected int[] links;

	protected Dimension origSize;

	

	public static class SelectNodeEvent implements EventListener {

		private TreeSourceWithSelectionSupport<?> selectionHandlder;
		private int nodeIndex;

		public SelectNodeEvent(TreeSourceWithSelectionSupport<?> selectionHandlder, int nodeIndex) {
			this.selectionHandlder = selectionHandlder;
			this.nodeIndex = nodeIndex;
		}

		@Override
		public void handleEvent(Event evt) {
			selectionHandlder.fireSlectionChanged(nodeIndex);
		}
		
	}
	
	public BatikTreeBuilder(TreeSourceWithSelectionSupport<E> treeSource) {
		this.treeSource = treeSource;
	}
	
	public SVGDocument getDoc() {
		return doc;
	}

	public Dimension getSize() {
		return origSize;
	}

	public void buildNewSvgTree() {
	
		fillDataStrucures();
		
		initBatik();
		
		//add nodes
		for (int j = 0; j < srcNodes.length; j++) {
			Element n = createSvgNode(j);
			svgNodes[j] = n;
		}
		
		if (srcNodes.length > 0) { 
		
			computeCoordinatesAndSpaceOutNodes();

			addLinks();
			addEdges();
		}
		
		fixTheFinalSize();
		
	
		if (treeSource.getSelectedCicle() != null) {
			colorNodeAsSelected(treeSource.getSelectedCicle());
		}
	}

	protected void fillDataStrucures() {
		cmp = new TreeComputation<>(treeSource);
		cmp.compute();
		
		links = cmp.collectLinks();
		edges = cmp.collectEdges();
		srcNodes = cmp.collectNodes();
		sortedNodes = cmp.computeSortedNodes();
		
		treeSource.setNodes(srcNodes);

		svgNodes = new Element[srcNodes.length];
		treeSource.setCircles(new Element[srcNodes.length]);		
	}

	protected void initBatik() {
		DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		doc = (SVGDocument) impl.createDocument(svgNS, "svg", null);
		
		// Get the root element (the 'svg' element).
		svgRoot = doc.getDocumentElement();

		mainGroupRoot = doc.createElementNS(svgNS, "g");
		svgRoot.appendChild(mainGroupRoot);

		edgesGroup = doc.createElementNS(svgNS, "g");
		mainGroupRoot.appendChild(edgesGroup);
		
		setupDynamicSvgBridge(doc);
	}

	protected void addSelfLink(int a, int b) {
		// Create circle.
		Element arc = buildElem("circle")
			.attr("cx", x[a]-Sizing.LINK_SELF_RADIUS+Sizing.LINK_STROKE_F*2f) 
			.attr("cy", y[a]-Sizing.LINK_SELF_RADIUS+Sizing.LINK_STROKE_F*2f) 
			.attr("r", Sizing.LINK_SELF_RADIUS) 
			.attr("fill", "none")
			.attr("stroke", 	  Color.EDGE[getEdgeType(a, b)])
			.attr("stroke-width", Sizing.LINK_STROKE)
			.attr("stroke-dasharray", Sizing.LINK_DASHARRAY)
			.get();
	
		// Attach
		edgesGroup.appendChild(arc);
	}

	protected void addLinks() {
		for (int i = 0; i < links.length; i+=2) {
			int a = links[i];
			int b = links[i+1];
			
			if (x[a] == x[b] && y[a] == y[b]) {
				addSelfLink(a, b);
				continue;
			}
			
			float sx = (x[b] + x[a])/2.0f;
			float sy = (y[b] + y[a])/2.0f;
			float norm_x = -(y[b] - y[a]);
			float norm_y = (x[b] - x[a]);
			float norm = norm_x*norm_x + norm_y*norm_y; 
			if (norm != 0) {
				norm = (float) Math.sqrt(norm);
				norm_x /= norm;
				norm_x /= norm;
			}
			float bezier_end_x = sx + norm_x*Sizing.LINK_END_SHIFT*0.03f;
			float bezier_end_y = sy + norm_y*Sizing.LINK_END_SHIFT*0.03f;

			float back_sx = (bezier_end_x - x[b])/10f;
			float back_sy = (bezier_end_y - y[b])/10f;
			
			String arcStr = new StringBuilder("M")
				//start point
				.append(x[a]).append(',').append(y[a]).append(" C")
				//control point at the beginning
				.append(x[a]).append(',').append(y[a]).append(" ")
				//control point at the end
				.append(bezier_end_x).append(',').append(bezier_end_y).append(" ")
				//end point
				.append(x[b]+back_sx).append(',').append(y[b]+back_sy)
				.toString();
			
			// Create path.
			Element arc = buildElem("path")
				.attr("d", arcStr) 
				.attr("fill", "none")
				.attr("stroke", 	  Color.EDGE[getEdgeType(a, b)])
				.attr("stroke-width", Sizing.LINK_STROKE)
				.attr("stroke-dasharray", Sizing.LINK_DASHARRAY)
				.get();

			// Attach
			edgesGroup.appendChild(arc);
		}
		
	}

	protected void addEdges() {
		for (int i = 0; i < edges.length; i+=2) {
			int a = edges[i];
			int b = edges[i+1];
			
			// Create line.
			Element line = buildElem("line")
				.attr("x1", x[a])
				.attr("y1", y[a])
				.attr("x2", x[b])
				.attr("y2", y[b])
				.attr("stroke", 	  Color.EDGE[getEdgeType(a, b)])
				.attr("stroke-width", Sizing.EDGE_STROKE)
				.get();

			// Attach
			edgesGroup.appendChild(line);
		}
	}

	protected void computeCoordinatesAndSpaceOutNodes() {
		//prepare sizes 
		SVGRect[] svgNodeBoxes = Arrays.stream(svgNodes)
				.map(BatikTreeBuilder::getBBox)
				.toArray(SVGRect[]::new);
		
		float maxHeightPerDepth[] = new float[cmp.getMaxDepth()+1];
		for (int i = 0; i < svgNodes.length; i++) {
			int d = cmp.getDepth(i);
			float h = svgNodeBoxes[i].getHeight();
			maxHeightPerDepth[d] = Math.max(maxHeightPerDepth[d], h);
		}

		float yOffsetForDepth[] = new float[cmp.getMaxDepth()+1];
		for (int d = 1; d < yOffsetForDepth.length; d++) {
			yOffsetForDepth[d] = Sizing.NODE_V_SPACE + yOffsetForDepth[d-1] + maxHeightPerDepth[d-1];
		}

		float lastDirtyXOffsetForDepth[] = new float[cmp.getMaxDepth()+1];
		for (int i = 0; i < lastDirtyXOffsetForDepth.length; i++) {
			lastDirtyXOffsetForDepth[i] = -Float.MAX_VALUE;
		}
		
		
		//compute
		//x
		//the initial position of each node is centered around 0 x coordinate 
		x = new float[srcNodes.length];
		
		int n0 = sortedNodes[0];
		float w0 = svgNodeBoxes[n0].getWidth();
		float x0 = svgNodeBoxes[n0].getX();
		x[n0] = 0f;
		lastDirtyXOffsetForDepth[cmp.getDepth(n0)] = x0 + w0;  
		
		for (int j = 1; j < srcNodes.length; j++) {
			float prevX = x[sortedNodes[j-1]];
			int n = sortedNodes[j];
			
			//System.err.println(srcNodes[n] +" "+ treeSource.getLabels(srcNodes[n]).get(0).getMiddle());
			
			float width = svgNodeBoxes[n].getWidth();
			float origXN = svgNodeBoxes[n].getX();
			int d = cmp.getDepth(n);
			
			x[n] = prevX + Sizing.NODE_H_SPACE;
			
			if (x[n]+origXN < lastDirtyXOffsetForDepth[d] + Sizing.TEXT_H_SPACE) {
				x[n] = lastDirtyXOffsetForDepth[d] - origXN + Sizing.TEXT_H_SPACE;
			}
			
			lastDirtyXOffsetForDepth[d] = x[n] + width + origXN;
		}

		//y
		y = new float[srcNodes.length];

		for (int j = 0; j < srcNodes.length; j++) {
			
			y[j] = yOffsetForDepth[cmp.getDepth(j)];
				
			//space out nodes
			svgNodes[j].setAttributeNS(null, "transform", "translate("+x[j]+","+y[j]+")");
		}
	}

	protected int getEdgeType(int indexParent, int indexChild) {
		return treeSource.getEdgeType(srcNodes[indexParent], srcNodes[indexChild]);
	}
	
	protected int getNodeType(int index) {
		return treeSource.getNodeType(srcNodes[index]);
	}

	protected void fixTheFinalSize() {
		SVGRect box = getBBox(svgRoot);
		
		if (box == null) return;
		
		origSize = new Dimension((int)(box.getWidth()+Sizing.BORDER*2), (int)(box.getHeight()+Sizing.BORDER*2));
		
		Element frame = buildElem("rect")
				.attr("x", "0")
				.attr("y", "0")
				.attr("width", origSize.getWidth())
				.attr("height", origSize.getHeight())
				.attr("fill", Color.FRAME_FILL)
				.attr("opacity", Color.FRAME_OPACITY)				
				.get();
		svgRoot.insertBefore(frame, mainGroupRoot);
		
		float trX = Sizing.BORDER-box.getX();
		float trY = Sizing.BORDER-box.getY();
		mainGroupRoot.setAttributeNS(null, "transform", "translate("+trX+","+trY+")");
	}

	public static void setupDynamicSvgBridge(SVGDocument doc) {
		UserAgentAdapter userAgent = new UserAgentAdapter();
	    DocumentLoader loader = new DocumentLoader(userAgent);
	    BridgeContext ctx = new BridgeContext(userAgent, loader); 
	    ctx.setDynamicState(BridgeContext.DYNAMIC);
	    GVTBuilder builder = new GVTBuilder();
	    builder.build(ctx, doc); 		
	}
	
	public static SVGRect getBBox(Node elem) {  
		SVGLocatable loc = (SVGLocatable) elem;
		SVGRect box = loc.getBBox();
		return box;
	}
	
	public static void colorNodeAsSelected(Element circle) {
		circle.setAttributeNS(null, "fill", Color.NODE_FILL_SELECTED);
		circle.setAttributeNS(null, "stroke", Color.NODE_STROKE_SELECTED);
		circle.setAttributeNS(null, "stroke-width", Sizing.NODE_STROKE_SELECTED);
	}

	public static void colorNodeAsNotSelected(Element circle, int nodeType) {
		circle.setAttributeNS(null, "fill",	Color.NODE[nodeType]);
		circle.setAttributeNS(null, "stroke", Color.NODE_STROKE);
		circle.setAttributeNS(null, "stroke-width", Sizing.NODE_STROKE);
	}
	
	protected Element createSvgNode(int j) {
		Element nodeGroup = buildElem("g").get();
		mainGroupRoot.appendChild(nodeGroup);
		Element strokeGroup = buildElem("g").get();
		nodeGroup.appendChild(strokeGroup);
		
		// Create circle.
		Element circile = buildElem("circle")
				.attr("cx", 	0)
				.attr("cy", 	0)
				.attr("r", 		Sizing.NODE_DIAM/2)
				.attr("stroke", Color.NODE_STROKE)
				.attr("stroke-width", Sizing.NODE_STROKE)
				.attr("fill", 	Color.NODE[getNodeType(j)])
				.get(); 
				
		nodeGroup.appendChild(circile);
		
		
		//labels
		List<NodeLabel> labels = treeSource.getLabels(srcNodes[j]);
		int line = 0;
		for (NodeLabel nodeLabel : labels) {
			appendLabel(nodeGroup, strokeGroup, line, nodeLabel);
			line++;
		}
		
		
		//addEvent
		treeSource.getCircles()[j] = circile;
		EventTarget t = (EventTarget) nodeGroup;
		t.addEventListener("click", new SelectNodeEvent(treeSource, j), true);
		
		return nodeGroup;
	}

	protected void appendLabel(Element nodeGroup, Element strokeGroup, int line, NodeLabel nodeLabel) {
		int txtY = Sizing.FIRST_LINE_Y + line * Sizing.LINE_HEIGHT;
		
		float middleOffset = Sizing.TEXT_OFFSET_MIDDLE;

		String middleText = nodeLabel.getMiddle();
		
		if (middleText != null && !middleText.trim().isEmpty()) {
			Element txtElem = createNiceText(nodeGroup, strokeGroup, nodeLabel.getMiddle(),	"middle", 0, txtY);
			
			if (txtElem != null)
				middleOffset += getTextSize(txtElem)/2;
		}
		
		createNiceText(nodeGroup, strokeGroup, nodeLabel.getLeftPart(), "end",  -middleOffset, txtY);
		createNiceText(nodeGroup, strokeGroup, nodeLabel.getRightPart(),"start", middleOffset, txtY);
	}
	
	protected float getTextSize(Element txtElem) {
		SVGRect box = getBBox(txtElem);
		return box.getWidth();
	}

	protected Element createNiceText(Element nodeGroup, Element strokeGroup, String textContent, String anchor, float x, int y) {
		if (textContent == null || textContent.trim().isEmpty()) return null;
		
		String xStr = Float.toString(x);
		String yStr = Integer.toString(y);
		
		// Create text stroke
		//https://www.w3.org/People/Dean/svg/texteffects/index.html
		ElemBuilder textStroke = createTextElem(doc, textContent, anchor, xStr, yStr);
		textStroke.attr("fill", Color.TEXT_STROKE);
		textStroke.attr("stroke", Color.TEXT_STROKE);
		textStroke.attr("stroke-width", Sizing.FONT_STROKE);
		textStroke.attr("opacity", Color.TEXT_STROKE_OPACITY);				

		// Attach 
		strokeGroup.appendChild(textStroke.get());

		// Create text.
		ElemBuilder text = createTextElem(doc, textContent, anchor, xStr, yStr);
		text.attr("fill", Color.TEXT);

		// Attach 
		nodeGroup.appendChild(text.get());
		
		return text.get();
	}
	
	public static ElemBuilder createTextElem(SVGDocument doc, String textContent, String anchor, String xStr, String yStr) {
		ElemBuilder text = new ElemBuilder(doc.createElementNS(svgNS, "text"));
		text.textContent(textContent);
		//text.attr("font-weight", "bold");
		text.attr("alignment-baseline", "text-after-edge");
		text.attr("text-anchor", anchor);
		text.attr("font-family", "Arial");
		text.attr("font-size", Sizing.FONT_SIZE );
		
		text.attr("x", xStr);
		text.attr("y", yStr);
		
		return text;
	}

	public ElemBuilder buildElem(String elemName) {
		return new ElemBuilder(doc.createElementNS(svgNS, elemName));
	}
	
	public static class ElemBuilder {
		Element el;

		public ElemBuilder(Element el) {
			this.el = el;
		}

		public ElemBuilder textContent(String textContent) {
			el.setTextContent(textContent);
			return this;
		}

		public ElemBuilder attr(String attrName, Object attrValue) {
			el.setAttributeNS(null, attrName, attrValue.toString());
			return this;
		}

		public Element get() {
			return el;
		}
		
	}
	
}
