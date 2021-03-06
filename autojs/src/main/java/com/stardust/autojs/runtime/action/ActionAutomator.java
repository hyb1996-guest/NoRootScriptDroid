package com.stardust.autojs.runtime.action;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.stardust.autojs.runtime.AccessibilityBridge;
import com.stardust.autojs.runtime.JavascriptInterface;
import com.stardust.autojs.runtime.ScriptRuntime;
import com.stardust.automator.AccessibilityEventCommandHost;

/**
 * Created by Stardust on 2017/4/2.
 */

public class ActionAutomator {

    private static class PerformGlobalActionCommand implements AccessibilityEventCommandHost.Command {

        boolean result;
        private int mGlobalAction;

        PerformGlobalActionCommand(int globalAction) {
            mGlobalAction = globalAction;
        }

        @Override
        public void execute(AccessibilityService service, AccessibilityEvent event) {
            result = service.performGlobalAction(mGlobalAction);
        }
    }

    private AccessibilityBridge mAccessibilityBridge;
    private ScriptRuntime mScriptRuntime;

    public ActionAutomator(AccessibilityBridge accessibilityBridge, ScriptRuntime scriptRuntime) {
        mAccessibilityBridge = accessibilityBridge;
        mScriptRuntime = scriptRuntime;
    }

    @JavascriptInterface
    public ActionTarget text(String text, int i) {
        return new ActionTarget.TextActionTarget(text, i);
    }

    @JavascriptInterface
    public ActionTarget bounds(int left, int top, int right, int bottom) {
        return new ActionTarget.BoundsActionTarget(new Rect(left, top, right, bottom));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @JavascriptInterface
    public ActionTarget editable(int i) {
        mScriptRuntime.requiresApi(Build.VERSION_CODES.LOLLIPOP);
        return new ActionTarget.EditableActionTarget(i);
    }

    @JavascriptInterface
    public ActionTarget id(String id) {
        return new ActionTarget.IdActionTarget(id);
    }

    @JavascriptInterface
    public boolean click(ActionTarget target) {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_CLICK));
    }

    @JavascriptInterface
    public boolean longClick(ActionTarget target) {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_LONG_CLICK));
    }

    @JavascriptInterface
    public boolean scrollUp(ActionTarget target) {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD));
    }

    @JavascriptInterface
    public boolean scrollDown(ActionTarget target) {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD));
    }

    @JavascriptInterface
    public boolean scrollUp(int i) {
        return performAction(ActionFactory.createScrollAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, i));
    }

    @JavascriptInterface
    public boolean scrollDown(int i) {
        return performAction(ActionFactory.createScrollAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, i));
    }

    @JavascriptInterface
    public boolean scrollAllUp() {
        return performAction(ActionFactory.createScrollMaxAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD));
    }

    @JavascriptInterface
    public boolean scrollAllDown() {
        return performAction(ActionFactory.createScrollMaxAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD));
    }

    @JavascriptInterface
    public boolean focus(ActionTarget target) {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_FOCUS));
    }

    @JavascriptInterface
    public boolean select(ActionTarget target) {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_SELECT));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean setText(ActionTarget target, String text) {
        mScriptRuntime.requiresApi(Build.VERSION_CODES.LOLLIPOP);
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_SET_TEXT, text));
    }

    @JavascriptInterface
    public boolean back() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    @JavascriptInterface
    public boolean home() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    @JavascriptInterface
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean powerDialog() {
        mScriptRuntime.requiresApi(Build.VERSION_CODES.LOLLIPOP);
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
    }

    @JavascriptInterface
    public boolean notifications() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
    }

    @JavascriptInterface
    public boolean quickSettings() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
    }

    @JavascriptInterface
    public boolean recents() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
    }

    @JavascriptInterface
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean splitScreen() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
    }

    private boolean performGlobalAction(final int action) {
        ensureAccessibilityServiceEnabled();
        PerformGlobalActionCommand command = new PerformGlobalActionCommand(action);
        mAccessibilityBridge.getCommandHost().executeAndWaitForEvent(command);
        return command.result;
    }

    @JavascriptInterface
    public boolean paste(ActionTarget target) {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_PASTE));
    }

    private void ensureAccessibilityServiceEnabled() {
        mAccessibilityBridge.ensureServiceEnabled();
    }

    @SuppressWarnings("unchecked")
    private <T> T performAction(Action action) {
        ensureAccessibilityServiceEnabled();
        mAccessibilityBridge.getActionPerformHost().addAction(action);
        synchronized (action) {
            try {
                action.wait();
            } catch (InterruptedException e) {
                action.setValid(false);
                mScriptRuntime.stoppedByInterrupted(e);
            }
        }
        return (T) action.getResult();
    }
}
