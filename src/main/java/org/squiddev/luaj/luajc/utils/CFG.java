package org.squiddev.luaj.luajc.utils;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

import static org.objectweb.asm.Opcodes.ASM5;

public class CFG extends MethodVisitor {
	public static class LabelTextifier extends Textifier {
		public LabelTextifier() {
			super(Opcodes.ASM5);
		}

		public String labelName(Label l) {
			if (labelNames == null) {
				labelNames = new HashMap<Label, String>();
			}
			String name = labelNames.get(l);
			if (name == null) {
				name = "L" + labelNames.size();
				labelNames.put(l, name);
			}
			return name;
		}
	}

	private final LabelTextifier textifier = new LabelTextifier();
	private final PrintWriter writer;
	private String currentLabel;
	private boolean hasJump = false;


	public CFG(PrintWriter writer) {
		super(ASM5);
		this.writer = writer;

		writer.println("strict digraph {");
		writer.println("node [shape=box]");
	}

	@Override
	public void visitInsn(int opcode) {
		hasJump = false;
		textifier.visitInsn(opcode);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		hasJump = false;
		textifier.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		hasJump = false;
		textifier.visitVarInsn(opcode, var);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		hasJump = false;
		textifier.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		hasJump = false;
		textifier.visitFieldInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		hasJump = false;
		textifier.visitMethodInsn(opcode, owner, name, desc, itf);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		hasJump = false;
		textifier.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		hasJump = false;
		textifier.visitLdcInsn(cst);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		hasJump = false;
		textifier.visitIincInsn(var, increment);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		hasJump = false;
		textifier.visitMultiANewArrayInsn(desc, dims);
	}

	private static void addString(PrintWriter writer, List text) {
		for (Object str : text) {
			if (str instanceof List) {
				addString(writer, (List) str);
			} else {
				writer.append(str.toString().replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;").replace("\n", "<br />"));
			}
		}
	}

	@Override
	public void visitLabel(Label label) {
		if (currentLabel != null) {
			if (!hasJump) visitJump(label);
			writer.append(currentLabel).append("[label =<");
			addString(writer, textifier.getText());
			writer.println(">];\n");
			textifier.getText().clear();
		}

		currentLabel = textifier.labelName(label);
		textifier.visitLabel(label);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		textifier.visitJumpInsn(opcode, label);
		visitJump(label);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		textifier.visitTableSwitchInsn(min, max, dflt, labels);
		for (Label label : labels) {
			visitJump(label);
		}
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		textifier.visitLookupSwitchInsn(dflt, keys, labels);
		for (Label label : labels) {
			visitJump(label);
		}
	}

	private void visitJump(Label label) {
		hasJump = true;
		writer.append(currentLabel).append("->").append(textifier.labelName(label)).append(";\n");
	}

	@Override
	public void visitEnd() {
		if (currentLabel != null) {
			writer.append(currentLabel).append("[label =<");
			addString(writer, textifier.getText());
			writer.println(">];\n");
			textifier.getText().clear();
		}
		currentLabel = null;
		writer.println("}");
	}
}
