package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

/**
 * Android 平台专属：拦截 XHR, Fetch 和 Form POST 请求的 JS 脚本
 */
val JS_INTERCEPT_POST = """
(function() {
    var bridge = window.WebPostService;
    if (!bridge || window._postInterceptInjected) return;
    window._postInterceptInjected = true;

    var requestIdHeader = 'X-WebView-Post-Id';
    var requestIdParam = '_webview_post_id';

    function register(id, body, contentType) {
        try {
            if (window.WebPostService && window.WebPostService.register) {
                window.WebPostService.register(id, body, contentType || '');
            }
        } catch(e) {
            console.error("WebPostService register error:", e);
        }
    }

    // The internal request-id header is not a CORS-safelisted header. Adding it
    // to a cross-origin request triggers a preflight that many academic systems
    // (including SEU's HTTP page -> HTTPS API flow) reject. Only requests that
    // the native interceptor can safely replay as same-origin may carry it.
    function isSameOriginTarget(url) {
        if (!url) return false;
        try {
            return new URL(url, window.location.href).origin === window.location.origin;
        } catch (e) {
            return false;
        }
    }

    // --- 1. XMLHttpRequest Interception ---
    var oldOpen = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function(method, url) {
        this._method = method;
        this._url = url;
        this._headers = {};
        return oldOpen.apply(this, arguments);
    };

    var oldSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;
    XMLHttpRequest.prototype.setRequestHeader = function(header, value) {
        try {
            if (this._headers && header) {
                this._headers[header] = value;
            }
            if (header && header.toLowerCase() === 'x-requested-with') return;
        } catch(e) {
            console.error("XHR setRequestHeader error:", e);
        }
        return oldSetRequestHeader.apply(this, arguments);
    };

    var oldSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function(body) {
        try {
            if (this._method && this._method.toUpperCase() !== 'GET' && body && isSameOriginTarget(this._url)) {
                var id = 'xhr_' + Date.now() + '_' + Math.random().toString(36).substr(2);
                var contentType = (this._headers && (this._headers['Content-Type'] || this._headers['content-type'])) || '';
                
                var bodyStr = '';
                if (typeof body === 'string') {
                    bodyStr = body;
                } else if (window.FormData && body instanceof FormData) {
                    if (window.URLSearchParams) {
                        var params = new URLSearchParams();
                        for (var pair of body.entries()) {
                            params.append(pair[0], pair[1]);
                        }
                        bodyStr = params.toString();
                    }
                    if (!contentType) contentType = 'application/x-www-form-urlencoded';
                } else if (window.URLSearchParams && body instanceof URLSearchParams) {
                    bodyStr = body.toString();
                    if (!contentType) contentType = 'application/x-www-form-urlencoded';
                }
                
                if (bodyStr) {
                    register(id, bodyStr, contentType);
                    this.setRequestHeader(requestIdHeader, id);
                }
            }
        } catch(e) {
            console.error("XHR send intercept error:", e);
        }
        return oldSend.apply(this, arguments);
    };

    // --- 2. Fetch API Interception ---
    if (window.fetch) {
        var oldFetch = window.fetch;
        window.fetch = function(input, init) {
            try {
                var options = init || {};
                var method = options.method;
                
                if (!method && input && typeof input === 'object' && input.method) {
                    method = input.method;
                }
                if (!method) method = 'GET';

                var body = options.body;
                var requestUrl = typeof input === 'string'
                    ? input
                    : (input && typeof input === 'object' ? input.url : '');

                if (method.toUpperCase() !== 'GET' && body && isSameOriginTarget(requestUrl)) {
                    var id = 'fetch_' + Date.now() + '_' + Math.random().toString(36).substr(2);
                    var contentType = '';
                    
                    // 提取 Content-Type
                    var headers = options.headers || (input && typeof input === 'object' ? input.headers : null);
                    if (headers) {
                        if (window.Headers && headers instanceof Headers) {
                            contentType = headers.get('Content-Type') || headers.get('content-type') || '';
                        } else if (Array.isArray(headers)) {
                            for (var i = 0; i < headers.length; i++) {
                                if (headers[i] && headers[i][0] && headers[i][0].toLowerCase() === 'content-type') {
                                    contentType = headers[i][1];
                                    break;
                                }
                            }
                        } else if (typeof headers === 'object') {
                            contentType = headers['Content-Type'] || headers['content-type'] || '';
                        }
                    }

                    var bodyStr = '';
                    if (typeof body === 'string') {
                        bodyStr = body;
                    } else if (window.URLSearchParams && body instanceof URLSearchParams) {
                        bodyStr = body.toString();
                        if (!contentType) contentType = 'application/x-www-form-urlencoded';
                    } else if (window.FormData && body instanceof FormData) {
                        if (window.URLSearchParams) {
                            var p = new URLSearchParams();
                            for (var pair of body.entries()) {
                                p.append(pair[0], pair[1]);
                            }
                            bodyStr = p.toString();
                        }
                        if (!contentType) contentType = 'application/x-www-form-urlencoded';
                    }
                    
                    if (bodyStr) {
                        register(id, bodyStr, contentType);
                        
                        // 注入 Request ID Header
                        if (!options.headers) options.headers = {};
                        if (window.Headers && options.headers instanceof Headers) {
                            options.headers.set(requestIdHeader, id);
                        } else if (Array.isArray(options.headers)) {
                            options.headers.push([requestIdHeader, id]);
                        } else if (typeof options.headers === 'object') {
                            options.headers[requestIdHeader] = id;
                        }
                    }
                }
                return oldFetch.call(this, input, options);
            } catch(e) {
                console.error("Fetch intercept error:", e);
                return oldFetch.apply(this, arguments);
            }
        };
    }

    // --- 3. Traditional Form Submit Interception ---
    document.addEventListener('submit', function(e) {
        try {
            var form = e.target;
            if (!form || !form.tagName || form.tagName.toLowerCase() !== 'form') return;
            if (!form.method || form.method.toLowerCase() !== 'post') return;

            if (form.querySelector && form.querySelector('input[type="file"]')) return;

            var id = 'form_' + Date.now() + '_' + Math.random().toString(36).substr(2);
            var formData = new FormData(form);
            
            var submitter = e.submitter || document.activeElement;
            if (submitter && submitter.form === form && submitter.name) {
                formData.append(submitter.name, submitter.value);
            }

            if (window.URLSearchParams) {
                var params = new URLSearchParams(formData);
                register(id, params.toString(), 'application/x-www-form-urlencoded');

                var action = form.getAttribute('action') || window.location.href;
                var separator = action.indexOf('?') !== -1 ? '&' : '?';
                form.setAttribute('action', action + separator + requestIdParam + '=' + id);
            }
        } catch(err) {
            console.error("Form submit intercept error:", err);
        }
    }, true);

    console.log('Unified X-Requested-With interceptor active');
})();
""".trimIndent()
