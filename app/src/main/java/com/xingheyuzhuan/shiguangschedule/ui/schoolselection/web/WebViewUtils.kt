package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.webkit.WebView

// 桌面模式的 User Agent
const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

/**
 * 注入网页端交互所需的所有 JavaScript 代码。
 * 包括：
 * 1. 桌面模式下的视口和CSS强制调整。
 * 2. AndroidBridgePromise 垫片，用于支持 JS 中的 Promise 调用 Android 原生功能。
 *
 * @param isDesktopMode 当前是否为桌面模式。
 */
fun WebView.injectAllJavaScript(isDesktopMode: Boolean) {
    // 1. 桌面模式注入逻辑
    if (isDesktopMode) {
        val desktopWidth = 1920
        evaluateJavascript("""
            (function() {
                var desktopWidth = ${desktopWidth};
                
                // 1. 强制视口注入
                var existingMeta = document.querySelector('meta[name=viewport]');
                if (existingMeta) {
                    existingMeta.parentNode.removeChild(existingMeta);
                }
                
                var meta = document.createElement('meta');
                meta.setAttribute('name', 'viewport');
                meta.setAttribute('content', 'width=' + desktopWidth + ', initial-scale=0.5, maximum-scale=3.0, user-scalable=yes'); 
                
                var head = document.getElementsByTagName('head')[0];
                if (head) {
                    head.appendChild(meta);
                }

                // 2. 强制 CSS 注入
                var style = document.createElement('style');
                style.innerHTML = 'html, body { ' +
                                  'overflow-x: visible !important; ' + 
                                  'min-width: ' + desktopWidth + 'px !important; ' + 
                                  'width: auto !important; ' + 
                                  'position: static !important; ' + 
                                  'padding: 0 !important; margin: 0 !important;' +
                                  '}';
                var head = document.getElementsByTagName('head')[0];
                if (head) {
                    head.appendChild(style);
                }
            })();
        """, null)
    }

    // 2. 注入 Promise 垫片代码 (始终注入，因为 Bridge 依赖它)
    evaluateJavascript("""
        window._androidPromiseResolvers = {};
        window._androidPromiseRejectors = {};

        window._resolveAndroidPromise = function(promiseId, result) {
            if (window._androidPromiseResolvers[promiseId]) {
                window._androidPromiseResolvers[promiseId](result);
                delete window._androidPromiseResolvers[promiseId];
                delete window._androidPromiseRejectors[promiseId];
            }
        };

        window._rejectAndroidPromise = function(promiseId, error) {
            if (window._androidPromiseRejectors[promiseId]) {
                window._androidPromiseRejectors[promiseId](new Error(error));
                delete window._androidPromiseResolvers[promiseId];
                delete window._androidPromiseRejectors[promiseId];
            }
        };

        window.AndroidBridgePromise = {
            showAlert: function(title, content, confirmText) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'alert_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.showAlert(title, content, confirmText, promiseId);
                });
            },
            showPrompt: function(title, tip, defaultText, validatorJsFunction) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'prompt_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.showPrompt(title, tip, defaultText, validatorJsFunction, promiseId);
                });
            },
            showSingleSelection: function(title, itemsJsonString, defaultSelectedIndex) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'singleSelect_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.showSingleSelection(title, itemsJsonString, defaultSelectedIndex, promiseId);
                });
            },
            saveImportedCourses: function(coursesJsonString) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'saveCourses_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.saveImportedCourses(coursesJsonString, promiseId);
                });
            },
            savePresetTimeSlots: function(timeSlotsJsonString) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'saveTimeSlots_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.savePresetTimeSlots(timeSlotsJsonString, promiseId);
                });
            }
        };
    """, null)
}