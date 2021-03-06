/*******************************************************************************
 * Copyright (c) 2007 Exadel, Inc. and Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
 ******************************************************************************/ 
package org.jboss.tools.common.meta.action.impl.handlers;

import java.util.*;
import org.jboss.tools.common.meta.action.*;
import org.jboss.tools.common.meta.action.impl.*;
import org.jboss.tools.common.meta.key.WizardKeys;
import org.jboss.tools.common.model.*;
import org.jboss.tools.common.model.plugin.ModelMessages;
import org.jboss.tools.common.model.util.*;
import org.jboss.tools.common.model.filesystems.XFileObject;

public class OpenWithChoiceHandler extends AbstractHandler {

    public OpenWithChoiceHandler() {}

    public boolean isEnabled(XModelObject object) {
        if(object == null || object.getFileType() != XFileObject.FILE) return false;
        return check(object);
    }

    public void executeHandler(XModelObject object, Properties p) throws XModelException {
        if(!isEnabled(object)) return;
		String displayName = WizardKeys.getMenuItemDisplayName(action, object == null ? null : object.getModelEntity());
        if(!OpenWithExternalHandler.checkSave(displayName, object)) return;
        ServiceDialog d = object.getModel().getService();
        String ext = OpenWithHelper.getLogicalExtension(object, action);
        XAttributeData a1 = HUtil.find(data, 0, XModelObjectConstants.ATTR_NAME);
        XAttributeData a2 = HUtil.find(data, 0, "default"); //$NON-NLS-1$
        XModelObject o = OpenWithHelper.getEditorObject(object.getModel(), ext);
        String oldname = null;
        if(o != null) {
            oldname = o.get(XModelObjectConstants.XML_ATTR_NAME);
            a1.setValue(oldname);
            a2.setValue(XModelObjectConstants.YES);
        } else {
            a2.setValue(XModelObjectConstants.NO);
        }
        int i = d.showDialog("Open With", "Select external program", new String[]{ModelMessages.OK, ModelMessages.Cancel}, data[0], ServiceDialog.QUESTION);
        if(i != 0) return;
        DefaultCreateHandler.extractProperties(data[0]);
        String en = HUtil.getValue(data, 0, XModelObjectConstants.ATTR_NAME);
        boolean def = XModelObjectConstants.YES.equals(HUtil.getValue(data, 0, "default")); //$NON-NLS-1$
        if(def && !en.equals(oldname)) {
            changeDefaultEditor(object.getModel(), ext, en);
        } else if(!def && en.equals(oldname)) {
            removeDefaultEditor(object.getModel(), ext);
        }
        XModelObject editor = object.getModel().getByPath(OpenWithHelper.EDITORS + XModelObjectConstants.SEPARATOR + en);
        String f = OpenWithHelper.getFileName(object);
        OpenWithExternalHandler.start(displayName, f, editor);
    }

    protected boolean check(XModelObject object) {
        String[] es = OpenWithHelper.getEditorList(object.getModel());
        HUtil.hackAttributeConstraintList(data, 0, XModelObjectConstants.ATTR_NAME, es);
        return es.length > 0;
    }

    static void changeDefaultEditor(XModel model, String ext, String editor) throws XModelException {
        XModelObject o = model.getByPath(OpenWithHelper.EDITORS);
        StringBuffer sb = new StringBuffer();
        String ov = o.getAttributeValue("extensions"); //$NON-NLS-1$
        String[] es = XModelObjectUtil.asStringArray(ov);
        boolean done = false;
        for (int i = 0; i < es.length; i++) {
            if(!es[i].toLowerCase().startsWith(ext.toLowerCase() + ":")) { //$NON-NLS-1$
                append(sb, es[i]);
            } else {
                done = true;
                append(sb, ext + ":" + editor); //$NON-NLS-1$
            }
        }
        if(!done) append(sb, ext + ":" + editor); //$NON-NLS-1$
        applyChange(o, ov, sb.toString());
    }

    static void removeDefaultEditor(XModel model, String ext) throws XModelException {
        XModelObject o = model.getByPath(OpenWithHelper.EDITORS);
        StringBuffer sb = new StringBuffer();
        String ov = o.getAttributeValue("extensions"); //$NON-NLS-1$
        String[] es = XModelObjectUtil.asStringArray(ov);
        for (int i = 0; i < es.length; i++) {
            if(!es[i].toLowerCase().startsWith(ext.toLowerCase() + ":")) { //$NON-NLS-1$
                append(sb, es[i]);
            }
        }
        applyChange(o, ov, sb.toString());
    }

    private static void append(StringBuffer sb, String item) {
        if(sb.length() > 0) sb.append(';');
        sb.append(item);
    }

    private static void applyChange(XModelObject editors, String ov, String nv) throws XModelException {
        if(nv.equals(ov)) return;
        editors.getModel().changeObjectAttribute(editors, "extensions", nv); //$NON-NLS-1$
        editors.getModel().saveOptions();
    }

}
