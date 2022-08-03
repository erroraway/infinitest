/*
 * Infinitest, a Continuous Test Runner.
 *
 * Copyright (C) 2010-2013
 * "Ben Rady" <benrady@gmail.com>,
 * "Rod Coffin" <rfciii@gmail.com>,
 * "Ryan Breidenbach" <ryan.breidenbach@gmail.com>
 * "David Gageot" <david@gageot.net>, et al.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.infinitest.intellij.plugin.launcher;

import org.infinitest.intellij.idea.language.InfinitestHighlightingPassFactory;
import org.infinitest.intellij.idea.language.InfinitestLineMarkersPass;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;

public class FileEditorListener implements PresenterListener {
	FileEditorManager fileEditorManager;

	public FileEditorListener(FileEditorManager fileEditorManager) {
		this.fileEditorManager = fileEditorManager;
	}

	@Override
	public void testRunCompleted() {
		for (FileEditor fileEditor : fileEditorManager.getSelectedEditors()) {
			if (fileEditor instanceof TextEditor) {
				TextEditor textEditor = (TextEditor) fileEditor;
				Editor editor = textEditor.getEditor();
				
				InfinitestLineMarkersPass pass = editor.getUserData(InfinitestHighlightingPassFactory.KEY);

				if (pass != null) {
					pass.applyInformationToEditor();
				}
			}
		}
	}

	@Override
	public void testRunSucceed() {
		// nothing to do here
	}

	@Override
	public void testRunFailed() {
		// nothing to do here
	}

	@Override
	public void testRunStarted() {
		// nothing to do here
	}

	@Override
	public void testRunWaiting() {
		// nothing to do here
	}
}
