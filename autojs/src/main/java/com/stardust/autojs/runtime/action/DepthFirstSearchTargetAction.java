package com.stardust.autojs.runtime.action;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Created by Stardust on 2017/1/27.
 */

public class DepthFirstSearchTargetAction extends SearchTargetAction {


    private Able mAble;

    public DepthFirstSearchTargetAction(int action, Filter filter) {
        super(action, filter);
        mAble = Able.ABLE_MAP.get(action);
    }


    @Override
    public AccessibilityNodeInfo searchTarget(AccessibilityNodeInfo n) {
        if (n == null)
            return null;
        if (mAble.isAble(n))
            return n;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo child = n.getChild(i);
            if (child == null)
                continue;
            AccessibilityNodeInfo node = searchTarget(child);
            if (node != null)
                return node;
            else
                child.recycle();
        }
        return null;
    }


}
