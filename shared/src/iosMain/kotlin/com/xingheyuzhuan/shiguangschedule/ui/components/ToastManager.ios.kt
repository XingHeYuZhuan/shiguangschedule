package com.xingheyuzhuan.shiguangschedule.ui.components

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSLog
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionCurveEaseOut
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_SEC
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

private const val TOAST_TAG = 20260811L
private const val MIN_VISIBLE_SECONDS = 1.0
private const val DEFAULT_VISIBLE_SECONDS = 1.8
private const val FADE_IN_SECONDS = 0.2
private const val FADE_OUT_SECONDS = 0.25

private val pendingToastMessages = mutableListOf<String>()
private var currentToastView: UIView? = null
private var currentToastGeneration = 0L
private var minimumDisplayElapsed = false
private var isDismissingToast = false

private fun keyWindow(): UIWindow? {
    val windowScene = UIApplication.sharedApplication.connectedScenes
        .firstOrNull { it is UIWindowScene } as? UIWindowScene

    return windowScene?.windows
        .orEmpty()
        .map { it as? UIWindow }
        .firstOrNull { it?.isKeyWindow() == true }
}

actual fun showPlatformToast(message: String) {
    NSLog(message)

    dispatch_async(dispatch_get_main_queue()) {
        val window = keyWindow() ?: return@dispatch_async
        pendingToastMessages.add(message)

        if (currentToastView == null && !isDismissingToast) {
            showNextToast(window)
        } else if (minimumDisplayElapsed) {
            dismissCurrentToast(currentToastGeneration)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun showNextToast(window: UIWindow) {
    if (pendingToastMessages.isEmpty()) return

    val message = pendingToastMessages.removeAt(0)
    currentToastGeneration += 1
    val generation = currentToastGeneration
    minimumDisplayElapsed = false
    isDismissingToast = false

    window.subviews
        .mapNotNull { it as? UIView }
        .filter { it.tag == TOAST_TAG }
        .forEach { it.removeFromSuperview() }

    val toastView = createToastView(window, message)
    currentToastView = toastView
    window.addSubview(toastView)

    UIView.animateWithDuration(
        duration = FADE_IN_SECONDS,
        animations = { toastView.alpha = 1.0 },
        completion = null
    )

    dispatchAfter(MIN_VISIBLE_SECONDS) {
        if (generation != currentToastGeneration) return@dispatchAfter
        minimumDisplayElapsed = true
        if (pendingToastMessages.isNotEmpty()) {
            dismissCurrentToast(generation)
        }
    }

    dispatchAfter(DEFAULT_VISIBLE_SECONDS) {
        if (generation == currentToastGeneration) {
            dismissCurrentToast(generation)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun createToastView(window: UIWindow, message: String): UIView {
    val windowWidth = window.bounds.useContents { size.width }
    val windowHeight = window.bounds.useContents { size.height }
    val safeBottom = window.safeAreaInsets.useContents { bottom }
    val horizontalMargin = 32.0
    val horizontalPadding = 16.0
    val verticalPadding = 10.0

    val measuringLabel = UILabel().apply {
        text = message
        textColor = UIColor.whiteColor
        textAlignment = NSTextAlignmentCenter
        numberOfLines = 0
        font = platform.UIKit.UIFont.systemFontOfSize(15.0)
        sizeToFit()
    }

    val measuredWidth = measuringLabel.bounds.useContents { size.width }
    val measuredHeight = measuringLabel.bounds.useContents { size.height }
    val toastWidth = minOf(windowWidth - horizontalMargin * 2, measuredWidth + horizontalPadding * 2)
    val toastHeight = measuredHeight + verticalPadding * 2
    val toastX = (windowWidth - toastWidth) / 2.0
    val toastY = windowHeight - safeBottom - toastHeight - 48.0

    val toastView = UIView(frame = CGRectMake(toastX, toastY, toastWidth, toastHeight)).apply {
        tag = TOAST_TAG
        alpha = 0.0
        userInteractionEnabled = false
        backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.82)
        layer.cornerRadius = 10.0
        clipsToBounds = true
    }

    val label = UILabel(
        frame = CGRectMake(
            horizontalPadding,
            verticalPadding,
            toastWidth - horizontalPadding * 2,
            toastHeight - verticalPadding * 2
        )
    ).apply {
        text = message
        userInteractionEnabled = false
        textColor = UIColor.whiteColor
        textAlignment = NSTextAlignmentCenter
        numberOfLines = 0
        font = platform.UIKit.UIFont.systemFontOfSize(15.0)
    }
    toastView.addSubview(label)

    return toastView
}

private fun dismissCurrentToast(generation: Long) {
    val toastView = currentToastView ?: return
    if (generation != currentToastGeneration || isDismissingToast) return

    isDismissingToast = true
    UIView.animateWithDuration(
        duration = FADE_OUT_SECONDS,
        delay = 0.0,
        options = UIViewAnimationOptionCurveEaseOut,
        animations = { toastView.alpha = 0.0 },
        completion = {
            toastView.removeFromSuperview()
            if (generation == currentToastGeneration) {
                currentToastView = null
                minimumDisplayElapsed = false
                isDismissingToast = false
                keyWindow()?.let { showNextToast(it) }
            }
        }
    )
}

private fun dispatchAfter(seconds: Double, block: () -> Unit) {
    val delayNanos = (seconds * NSEC_PER_SEC.toDouble()).toLong()
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delayNanos), dispatch_get_main_queue(), block)
}
