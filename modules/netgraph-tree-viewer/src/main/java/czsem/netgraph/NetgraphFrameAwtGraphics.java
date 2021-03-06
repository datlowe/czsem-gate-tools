package czsem.netgraph;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import czsem.netgraph.batik.BatikTreeBuilder;
import czsem.netgraph.treesource.TreeSourceWithSelectionSupport;

public class NetgraphFrameAwtGraphics {
	
	public static class TestSource extends TreeSourceWithSelectionSupport<Integer> {

		@Override
		public Integer getRoot() {
			return 3;
		}

		@Override
		public List<Integer> getChildren(Integer parent) {
			switch (parent) {
			case 3:
				return Arrays.asList(1, 5, 7);
			case 1:
				return Arrays.asList(2);
			case 5:
				return Arrays.asList(4);
			case 7:
				return Arrays.asList(6, /*debug cycles*/ 3, 4);
				//return Arrays.asList(6);
			//debug cycles
			case 4:
				return Arrays.asList(4);
			}
			
			return Collections.emptyList();
		}
		
		private static final NodeLabel labels[][] = {
			null, //0
			{
				new StaticLabel("MMMMMMMMManželka"),
				new StaticLabel("ACT"),
				new StaticLabel("irregular"),
			}, //1
			{
				new StaticLabel("klineta"),
				new StaticLabel("shift", "=", "looooong text"),
				new StaticLabel("APP"),
			}, //2
			{
				new StaticLabel("má"),
				new StaticLabel("PRED"),
			}, //3
			{
				new StaticLabel("nějaký"),
				new StaticLabel("RSTR"),
			}, //4
			{
				new StaticLabel("úvěr"),
				new StaticLabel("PAT"),
			}, //5
			{
				new StaticLabel("nejen"),
				new StaticLabel("FPHR"),
				new StaticLabel("t_lemma", "=", "nejen"),
				new StaticLabel("tttt_lemma", "==", "ne"),
				new StaticLabel("tttt_lemma", "======", "ne"),
				new StaticLabel("a", ".", "bbbbbbbb3210gpy"),
			}, //6
			{
				new StaticLabel("ČSOB"),
				new StaticLabel("LOC"),
			}, //7
			
		};
		
		private static final int NODE_TYPES[] = {
			BatikTreeBuilder.NodeType.STANDARD,
			BatikTreeBuilder.NodeType.STANDARD,
			BatikTreeBuilder.NodeType.OPTIONAL,
			BatikTreeBuilder.NodeType.EMPHASIZED,
			BatikTreeBuilder.NodeType.WARN,
			BatikTreeBuilder.NodeType.EMPHASIZED,
			BatikTreeBuilder.NodeType.STANDARD,
			BatikTreeBuilder.NodeType.EMPHASIZED,
		};
		
		@Override
		public int getNodeType(Integer node) {
			return NODE_TYPES[node];
		}
		

		@Override
		public List<NodeLabel> getLabels(Integer node) {
			//return Collections.singletonList(new MiddleLabel(node.toString()));
			//return Collections.singletonList(new MiddleLabel(""));
			return Arrays.asList(labels[node]);
		}

		@Override
		public Comparator<Integer> getOrderComparator() {
			//return null;
			return Integer::compare;
		}
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("NetgraphFrame");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
		frame.setSize(700, 500);
		frame.add(new NetgraphViewAwtGraphics<>(new TestSource()));
		frame.pack();
		frame.setVisible(true);

	}

}
