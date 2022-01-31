/******/ (() => { // webpackBootstrap
/******/ 	var __webpack_modules__ = ({

/***/ "../ui-trellis/lib/components/containers/copybox/CopyBox.vue.js":
/*!**********************************************************************!*\
  !*** ../ui-trellis/lib/components/containers/copybox/CopyBox.vue.js ***!
  \**********************************************************************/
/***/ ((module) => {

function _typeof(obj) { "@babel/helpers - typeof"; return _typeof = "function" == typeof Symbol && "symbol" == typeof Symbol.iterator ? function (obj) { return typeof obj; } : function (obj) { return obj && "function" == typeof Symbol && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }, _typeof(obj); }

(function () {
  var e = {
    7757: function _(e, t, n) {
      e.exports = n(5666);
    },
    9505: function _(e, t, n) {
      "use strict";

      n.d(t, {
        CopyToClipboard: function CopyToClipboard() {
          return a;
        }
      });
      var r = n(5861),
          i = n(7757),
          o = n.n(i);

      function a(e) {
        return s.apply(this, arguments);
      }

      function s() {
        return (s = (0, r.Z)(o().mark(function e(t) {
          var n;
          return o().wrap(function (e) {
            for (;;) {
              switch (e.prev = e.next) {
                case 0:
                  if (!navigator.clipboard || !navigator.clipboard.writeText) {
                    e.next = 5;
                    break;
                  }

                  return e.next = 3, navigator.clipboard.writeText(t);

                case 3:
                  e.next = 14;
                  break;

                case 5:
                  return (n = document.createElement("textarea")).value = t, n.style.position = "fixed", n.style.left = "-999999px", n.style.top = "-999999px", document.body.appendChild(n), n.focus(), n.select(), e.abrupt("return", new Promise(function (e, t) {
                    document.execCommand("copy") ? e(void 0) : t(), n.remove();
                  }));

                case 14:
                case "end":
                  return e.stop();
              }
            }
          }, e);
        }))).apply(this, arguments);
      }
    },
    1621: function _(e, t, n) {
      "use strict";

      n.d(t, {
        Z: function Z() {
          return o;
        }
      });
      var r = n(3645),
          i = n.n(r)()(function (e) {
        return e[1];
      });
      i.push([e.id, ".rd-copybox[data-v-bfe70aba]{background-color:var(--background-color-lvl2);border:.1em solid var(--border-color);border-radius:5px;cursor:pointer;display:flex;position:relative}.rd-copybox__content[data-v-bfe70aba]{border-bottom-left-radius:5px;border-top-left-radius:5px;font-weight:600}.rd-copybox__content[data-v-bfe70aba],.rd-copybox__spacer[data-v-bfe70aba]{overflow:hidden;padding:10px;transition:all .2s ease-in-out;transition-property:color,background-color}.rd-copybox__content--active[data-v-bfe70aba],.rd-copybox__spacer--active[data-v-bfe70aba]{background-color:#708090;color:#228b22}.rd-copybox__spacer[data-v-bfe70aba]{flex-grow:1}.rd-copybox__icon[data-v-bfe70aba]{align-content:center;align-items:center;border-left:inherit;display:flex;padding:10px}.rd-copybox__success[data-v-bfe70aba]{color:var(--font-fill-color);font-weight:800;left:10px;opacity:0;position:absolute;top:10px;transition:opacity .2s ease-in-out}.rd-copybox__success--active[data-v-bfe70aba]{opacity:1}", ""]);
      var o = i;
    },
    3645: function _(e) {
      "use strict";

      e.exports = function (e) {
        var t = [];
        return t.toString = function () {
          return this.map(function (t) {
            var n = e(t);
            return t[2] ? "@media ".concat(t[2], " {").concat(n, "}") : n;
          }).join("");
        }, t.i = function (e, n, r) {
          "string" == typeof e && (e = [[null, e, ""]]);
          var i = {};
          if (r) for (var o = 0; o < this.length; o++) {
            var a = this[o][0];
            null != a && (i[a] = !0);
          }

          for (var s = 0; s < e.length; s++) {
            var c = [].concat(e[s]);
            r && i[c[0]] || (n && (c[2] ? c[2] = "".concat(n, " and ").concat(c[2]) : c[2] = n), t.push(c));
          }
        }, t;
      };
    },
    5666: function _(e) {
      var t = function (e) {
        "use strict";

        var t,
            n = Object.prototype,
            r = n.hasOwnProperty,
            i = "function" == typeof Symbol ? Symbol : {},
            o = i.iterator || "@@iterator",
            a = i.asyncIterator || "@@asyncIterator",
            s = i.toStringTag || "@@toStringTag";

        function c(e, t, n) {
          return Object.defineProperty(e, t, {
            value: n,
            enumerable: !0,
            configurable: !0,
            writable: !0
          }), e[t];
        }

        try {
          c({}, "");
        } catch (e) {
          c = function c(e, t, n) {
            return e[t] = n;
          };
        }

        function u(e, t, n, r) {
          var i = t && t.prototype instanceof m ? t : m,
              o = Object.create(i.prototype),
              a = new S(r || []);
          return o._invoke = function (e, t, n) {
            var r = f;
            return function (i, o) {
              if (r === d) throw new Error("Generator is already running");

              if (r === v) {
                if ("throw" === i) throw o;
                return E();
              }

              for (n.method = i, n.arg = o;;) {
                var a = n.delegate;

                if (a) {
                  var s = k(a, n);

                  if (s) {
                    if (s === h) continue;
                    return s;
                  }
                }

                if ("next" === n.method) n.sent = n._sent = n.arg;else if ("throw" === n.method) {
                  if (r === f) throw r = v, n.arg;
                  n.dispatchException(n.arg);
                } else "return" === n.method && n.abrupt("return", n.arg);
                r = d;
                var c = l(e, t, n);

                if ("normal" === c.type) {
                  if (r = n.done ? v : p, c.arg === h) continue;
                  return {
                    value: c.arg,
                    done: n.done
                  };
                }

                "throw" === c.type && (r = v, n.method = "throw", n.arg = c.arg);
              }
            };
          }(e, n, a), o;
        }

        function l(e, t, n) {
          try {
            return {
              type: "normal",
              arg: e.call(t, n)
            };
          } catch (e) {
            return {
              type: "throw",
              arg: e
            };
          }
        }

        e.wrap = u;
        var f = "suspendedStart",
            p = "suspendedYield",
            d = "executing",
            v = "completed",
            h = {};

        function m() {}

        function y() {}

        function g() {}

        var _ = {};
        c(_, o, function () {
          return this;
        });
        var b = Object.getPrototypeOf,
            w = b && b(b(T([])));
        w && w !== n && r.call(w, o) && (_ = w);
        var x = g.prototype = m.prototype = Object.create(_);

        function $(e) {
          ["next", "throw", "return"].forEach(function (t) {
            c(e, t, function (e) {
              return this._invoke(t, e);
            });
          });
        }

        function C(e, t) {
          function n(i, o, a, s) {
            var c = l(e[i], e, o);

            if ("throw" !== c.type) {
              var u = c.arg,
                  f = u.value;
              return f && "object" == _typeof(f) && r.call(f, "__await") ? t.resolve(f.__await).then(function (e) {
                n("next", e, a, s);
              }, function (e) {
                n("throw", e, a, s);
              }) : t.resolve(f).then(function (e) {
                u.value = e, a(u);
              }, function (e) {
                return n("throw", e, a, s);
              });
            }

            s(c.arg);
          }

          var i;

          this._invoke = function (e, r) {
            function o() {
              return new t(function (t, i) {
                n(e, r, t, i);
              });
            }

            return i = i ? i.then(o, o) : o();
          };
        }

        function k(e, n) {
          var r = e.iterator[n.method];

          if (r === t) {
            if (n.delegate = null, "throw" === n.method) {
              if (e.iterator["return"] && (n.method = "return", n.arg = t, k(e, n), "throw" === n.method)) return h;
              n.method = "throw", n.arg = new TypeError("The iterator does not provide a 'throw' method");
            }

            return h;
          }

          var i = l(r, e.iterator, n.arg);
          if ("throw" === i.type) return n.method = "throw", n.arg = i.arg, n.delegate = null, h;
          var o = i.arg;
          return o ? o.done ? (n[e.resultName] = o.value, n.next = e.nextLoc, "return" !== n.method && (n.method = "next", n.arg = t), n.delegate = null, h) : o : (n.method = "throw", n.arg = new TypeError("iterator result is not an object"), n.delegate = null, h);
        }

        function A(e) {
          var t = {
            tryLoc: e[0]
          };
          1 in e && (t.catchLoc = e[1]), 2 in e && (t.finallyLoc = e[2], t.afterLoc = e[3]), this.tryEntries.push(t);
        }

        function O(e) {
          var t = e.completion || {};
          t.type = "normal", delete t.arg, e.completion = t;
        }

        function S(e) {
          this.tryEntries = [{
            tryLoc: "root"
          }], e.forEach(A, this), this.reset(!0);
        }

        function T(e) {
          if (e) {
            var n = e[o];
            if (n) return n.call(e);
            if ("function" == typeof e.next) return e;

            if (!isNaN(e.length)) {
              var i = -1,
                  a = function n() {
                for (; ++i < e.length;) {
                  if (r.call(e, i)) return n.value = e[i], n.done = !1, n;
                }

                return n.value = t, n.done = !0, n;
              };

              return a.next = a;
            }
          }

          return {
            next: E
          };
        }

        function E() {
          return {
            value: t,
            done: !0
          };
        }

        return y.prototype = g, c(x, "constructor", g), c(g, "constructor", y), y.displayName = c(g, s, "GeneratorFunction"), e.isGeneratorFunction = function (e) {
          var t = "function" == typeof e && e.constructor;
          return !!t && (t === y || "GeneratorFunction" === (t.displayName || t.name));
        }, e.mark = function (e) {
          return Object.setPrototypeOf ? Object.setPrototypeOf(e, g) : (e.__proto__ = g, c(e, s, "GeneratorFunction")), e.prototype = Object.create(x), e;
        }, e.awrap = function (e) {
          return {
            __await: e
          };
        }, $(C.prototype), c(C.prototype, a, function () {
          return this;
        }), e.AsyncIterator = C, e.async = function (t, n, r, i, o) {
          void 0 === o && (o = Promise);
          var a = new C(u(t, n, r, i), o);
          return e.isGeneratorFunction(n) ? a : a.next().then(function (e) {
            return e.done ? e.value : a.next();
          });
        }, $(x), c(x, s, "Generator"), c(x, o, function () {
          return this;
        }), c(x, "toString", function () {
          return "[object Generator]";
        }), e.keys = function (e) {
          var t = [];

          for (var n in e) {
            t.push(n);
          }

          return t.reverse(), function n() {
            for (; t.length;) {
              var r = t.pop();
              if (r in e) return n.value = r, n.done = !1, n;
            }

            return n.done = !0, n;
          };
        }, e.values = T, S.prototype = {
          constructor: S,
          reset: function reset(e) {
            if (this.prev = 0, this.next = 0, this.sent = this._sent = t, this.done = !1, this.delegate = null, this.method = "next", this.arg = t, this.tryEntries.forEach(O), !e) for (var n in this) {
              "t" === n.charAt(0) && r.call(this, n) && !isNaN(+n.slice(1)) && (this[n] = t);
            }
          },
          stop: function stop() {
            this.done = !0;
            var e = this.tryEntries[0].completion;
            if ("throw" === e.type) throw e.arg;
            return this.rval;
          },
          dispatchException: function dispatchException(e) {
            if (this.done) throw e;
            var n = this;

            function i(r, i) {
              return s.type = "throw", s.arg = e, n.next = r, i && (n.method = "next", n.arg = t), !!i;
            }

            for (var o = this.tryEntries.length - 1; o >= 0; --o) {
              var a = this.tryEntries[o],
                  s = a.completion;
              if ("root" === a.tryLoc) return i("end");

              if (a.tryLoc <= this.prev) {
                var c = r.call(a, "catchLoc"),
                    u = r.call(a, "finallyLoc");

                if (c && u) {
                  if (this.prev < a.catchLoc) return i(a.catchLoc, !0);
                  if (this.prev < a.finallyLoc) return i(a.finallyLoc);
                } else if (c) {
                  if (this.prev < a.catchLoc) return i(a.catchLoc, !0);
                } else {
                  if (!u) throw new Error("try statement without catch or finally");
                  if (this.prev < a.finallyLoc) return i(a.finallyLoc);
                }
              }
            }
          },
          abrupt: function abrupt(e, t) {
            for (var n = this.tryEntries.length - 1; n >= 0; --n) {
              var i = this.tryEntries[n];

              if (i.tryLoc <= this.prev && r.call(i, "finallyLoc") && this.prev < i.finallyLoc) {
                var o = i;
                break;
              }
            }

            o && ("break" === e || "continue" === e) && o.tryLoc <= t && t <= o.finallyLoc && (o = null);
            var a = o ? o.completion : {};
            return a.type = e, a.arg = t, o ? (this.method = "next", this.next = o.finallyLoc, h) : this.complete(a);
          },
          complete: function complete(e, t) {
            if ("throw" === e.type) throw e.arg;
            return "break" === e.type || "continue" === e.type ? this.next = e.arg : "return" === e.type ? (this.rval = this.arg = e.arg, this.method = "return", this.next = "end") : "normal" === e.type && t && (this.next = t), h;
          },
          finish: function finish(e) {
            for (var t = this.tryEntries.length - 1; t >= 0; --t) {
              var n = this.tryEntries[t];
              if (n.finallyLoc === e) return this.complete(n.completion, n.afterLoc), O(n), h;
            }
          },
          "catch": function _catch(e) {
            for (var t = this.tryEntries.length - 1; t >= 0; --t) {
              var n = this.tryEntries[t];

              if (n.tryLoc === e) {
                var r = n.completion;

                if ("throw" === r.type) {
                  var i = r.arg;
                  O(n);
                }

                return i;
              }
            }

            throw new Error("illegal catch attempt");
          },
          delegateYield: function delegateYield(e, n, r) {
            return this.delegate = {
              iterator: T(e),
              resultName: n,
              nextLoc: r
            }, "next" === this.method && (this.arg = t), h;
          }
        }, e;
      }(e.exports);

      try {
        regeneratorRuntime = t;
      } catch (e) {
        "object" == (typeof globalThis === "undefined" ? "undefined" : _typeof(globalThis)) ? globalThis.regeneratorRuntime = t : Function("r", "regeneratorRuntime = r")(t);
      }
    },
    3379: function _(e, t, n) {
      "use strict";

      var r,
          i = function i() {
        return void 0 === r && (r = Boolean(window && document && document.all && !window.atob)), r;
      },
          o = function () {
        var e = {};
        return function (t) {
          if (void 0 === e[t]) {
            var n = document.querySelector(t);
            if (window.HTMLIFrameElement && n instanceof window.HTMLIFrameElement) try {
              n = n.contentDocument.head;
            } catch (e) {
              n = null;
            }
            e[t] = n;
          }

          return e[t];
        };
      }(),
          a = [];

      function s(e) {
        for (var t = -1, n = 0; n < a.length; n++) {
          if (a[n].identifier === e) {
            t = n;
            break;
          }
        }

        return t;
      }

      function c(e, t) {
        for (var n = {}, r = [], i = 0; i < e.length; i++) {
          var o = e[i],
              c = t.base ? o[0] + t.base : o[0],
              u = n[c] || 0,
              l = "".concat(c, " ").concat(u);
          n[c] = u + 1;
          var f = s(l),
              p = {
            css: o[1],
            media: o[2],
            sourceMap: o[3]
          };
          -1 !== f ? (a[f].references++, a[f].updater(p)) : a.push({
            identifier: l,
            updater: m(p, t),
            references: 1
          }), r.push(l);
        }

        return r;
      }

      function u(e) {
        var t = document.createElement("style"),
            r = e.attributes || {};

        if (void 0 === r.nonce) {
          var i = n.nc;
          i && (r.nonce = i);
        }

        if (Object.keys(r).forEach(function (e) {
          t.setAttribute(e, r[e]);
        }), "function" == typeof e.insert) e.insert(t);else {
          var a = o(e.insert || "head");
          if (!a) throw new Error("Couldn't find a style target. This probably means that the value for the 'insert' parameter is invalid.");
          a.appendChild(t);
        }
        return t;
      }

      var l,
          f = (l = [], function (e, t) {
        return l[e] = t, l.filter(Boolean).join("\n");
      });

      function p(e, t, n, r) {
        var i = n ? "" : r.media ? "@media ".concat(r.media, " {").concat(r.css, "}") : r.css;
        if (e.styleSheet) e.styleSheet.cssText = f(t, i);else {
          var o = document.createTextNode(i),
              a = e.childNodes;
          a[t] && e.removeChild(a[t]), a.length ? e.insertBefore(o, a[t]) : e.appendChild(o);
        }
      }

      function d(e, t, n) {
        var r = n.css,
            i = n.media,
            o = n.sourceMap;
        if (i ? e.setAttribute("media", i) : e.removeAttribute("media"), o && "undefined" != typeof btoa && (r += "\n/*# sourceMappingURL=data:application/json;base64,".concat(btoa(unescape(encodeURIComponent(JSON.stringify(o)))), " */")), e.styleSheet) e.styleSheet.cssText = r;else {
          for (; e.firstChild;) {
            e.removeChild(e.firstChild);
          }

          e.appendChild(document.createTextNode(r));
        }
      }

      var v = null,
          h = 0;

      function m(e, t) {
        var n, r, i;

        if (t.singleton) {
          var o = h++;
          n = v || (v = u(t)), r = p.bind(null, n, o, !1), i = p.bind(null, n, o, !0);
        } else n = u(t), r = d.bind(null, n, t), i = function i() {
          !function (e) {
            if (null === e.parentNode) return !1;
            e.parentNode.removeChild(e);
          }(n);
        };

        return r(e), function (t) {
          if (t) {
            if (t.css === e.css && t.media === e.media && t.sourceMap === e.sourceMap) return;
            r(e = t);
          } else i();
        };
      }

      e.exports = function (e, t) {
        (t = t || {}).singleton || "boolean" == typeof t.singleton || (t.singleton = i());
        var n = c(e = e || [], t);
        return function (e) {
          if (e = e || [], "[object Array]" === Object.prototype.toString.call(e)) {
            for (var r = 0; r < n.length; r++) {
              var i = s(n[r]);
              a[i].references--;
            }

            for (var o = c(e, t), u = 0; u < n.length; u++) {
              var l = s(n[u]);
              0 === a[l].references && (a[l].updater(), a.splice(l, 1));
            }

            n = o;
          }
        };
      };
    },
    5861: function _(e, t, n) {
      "use strict";

      function r(e, t, n, r, i, o, a) {
        try {
          var s = e[o](a),
              c = s.value;
        } catch (e) {
          return void n(e);
        }

        s.done ? t(c) : Promise.resolve(c).then(r, i);
      }

      function i(e) {
        return function () {
          var t = this,
              n = arguments;
          return new Promise(function (i, o) {
            var a = e.apply(t, n);

            function s(e) {
              r(a, i, o, s, c, "next", e);
            }

            function c(e) {
              r(a, i, o, s, c, "throw", e);
            }

            s(void 0);
          });
        };
      }

      n.d(t, {
        Z: function Z() {
          return i;
        }
      });
    }
  },
      t = {};

  function n(r) {
    var i = t[r];
    if (void 0 !== i) return i.exports;
    var o = t[r] = {
      id: r,
      exports: {}
    };
    return e[r](o, o.exports, n), o.exports;
  }

  n.n = function (e) {
    var t = e && e.__esModule ? function () {
      return e["default"];
    } : function () {
      return e;
    };
    return n.d(t, {
      a: t
    }), t;
  }, n.d = function (e, t) {
    for (var r in t) {
      n.o(t, r) && !n.o(e, r) && Object.defineProperty(e, r, {
        enumerable: !0,
        get: t[r]
      });
    }
  }, n.g = function () {
    if ("object" == (typeof globalThis === "undefined" ? "undefined" : _typeof(globalThis))) return globalThis;

    try {
      return this || new Function("return this")();
    } catch (e) {
      if ("object" == (typeof window === "undefined" ? "undefined" : _typeof(window))) return window;
    }
  }(), n.o = function (e, t) {
    return Object.prototype.hasOwnProperty.call(e, t);
  }, n.r = function (e) {
    "undefined" != typeof Symbol && Symbol.toStringTag && Object.defineProperty(e, Symbol.toStringTag, {
      value: "Module"
    }), Object.defineProperty(e, "__esModule", {
      value: !0
    });
  };
  var r = {};
  (function () {
    "use strict";

    n.r(r), n.d(r, {
      "default": function _default() {
        return Ds;
      }
    });
    var e = n(5861),
        t = n(7757),
        i = n.n(t),
        o = Object.freeze({});

    function a(e) {
      return null == e;
    }

    function s(e) {
      return null != e;
    }

    function c(e) {
      return !0 === e;
    }

    function u(e) {
      return "string" == typeof e || "number" == typeof e || "symbol" == _typeof(e) || "boolean" == typeof e;
    }

    function l(e) {
      return null !== e && "object" == _typeof(e);
    }

    var f = Object.prototype.toString;

    function p(e) {
      return "[object Object]" === f.call(e);
    }

    function d(e) {
      return "[object RegExp]" === f.call(e);
    }

    function v(e) {
      var t = parseFloat(String(e));
      return t >= 0 && Math.floor(t) === t && isFinite(e);
    }

    function h(e) {
      return s(e) && "function" == typeof e.then && "function" == typeof e["catch"];
    }

    function m(e) {
      return null == e ? "" : Array.isArray(e) || p(e) && e.toString === f ? JSON.stringify(e, null, 2) : String(e);
    }

    function y(e) {
      var t = parseFloat(e);
      return isNaN(t) ? e : t;
    }

    function g(e, t) {
      for (var n = Object.create(null), r = e.split(","), i = 0; i < r.length; i++) {
        n[r[i]] = !0;
      }

      return t ? function (e) {
        return n[e.toLowerCase()];
      } : function (e) {
        return n[e];
      };
    }

    var _ = g("slot,component", !0),
        b = g("key,ref,slot,slot-scope,is");

    function w(e, t) {
      if (e.length) {
        var n = e.indexOf(t);
        if (n > -1) return e.splice(n, 1);
      }
    }

    var x = Object.prototype.hasOwnProperty;

    function $(e, t) {
      return x.call(e, t);
    }

    function C(e) {
      var t = Object.create(null);
      return function (n) {
        return t[n] || (t[n] = e(n));
      };
    }

    var k = /-(\w)/g,
        A = C(function (e) {
      return e.replace(k, function (e, t) {
        return t ? t.toUpperCase() : "";
      });
    }),
        O = C(function (e) {
      return e.charAt(0).toUpperCase() + e.slice(1);
    }),
        S = /\B([A-Z])/g,
        T = C(function (e) {
      return e.replace(S, "-$1").toLowerCase();
    });
    var E = Function.prototype.bind ? function (e, t) {
      return e.bind(t);
    } : function (e, t) {
      function n(n) {
        var r = arguments.length;
        return r ? r > 1 ? e.apply(t, arguments) : e.call(t, n) : e.call(t);
      }

      return n._length = e.length, n;
    };

    function j(e, t) {
      t = t || 0;

      for (var n = e.length - t, r = new Array(n); n--;) {
        r[n] = e[n + t];
      }

      return r;
    }

    function L(e, t) {
      for (var n in t) {
        e[n] = t[n];
      }

      return e;
    }

    function N(e) {
      for (var t = {}, n = 0; n < e.length; n++) {
        e[n] && L(t, e[n]);
      }

      return t;
    }

    function M(e, t, n) {}

    var I = function I(e, t, n) {
      return !1;
    },
        D = function D(e) {
      return e;
    };

    function P(e, t) {
      if (e === t) return !0;
      var n = l(e),
          r = l(t);
      if (!n || !r) return !n && !r && String(e) === String(t);

      try {
        var i = Array.isArray(e),
            o = Array.isArray(t);
        if (i && o) return e.length === t.length && e.every(function (e, n) {
          return P(e, t[n]);
        });
        if (e instanceof Date && t instanceof Date) return e.getTime() === t.getTime();
        if (i || o) return !1;
        var a = Object.keys(e),
            s = Object.keys(t);
        return a.length === s.length && a.every(function (n) {
          return P(e[n], t[n]);
        });
      } catch (e) {
        return !1;
      }
    }

    function F(e, t) {
      for (var n = 0; n < e.length; n++) {
        if (P(e[n], t)) return n;
      }

      return -1;
    }

    function R(e) {
      var t = !1;
      return function () {
        t || (t = !0, e.apply(this, arguments));
      };
    }

    var H = "data-server-rendered",
        B = ["component", "directive", "filter"],
        U = ["beforeCreate", "created", "beforeMount", "mounted", "beforeUpdate", "updated", "beforeDestroy", "destroyed", "activated", "deactivated", "errorCaptured", "serverPrefetch"],
        V = {
      optionMergeStrategies: Object.create(null),
      silent: !1,
      productionTip: !1,
      devtools: !1,
      performance: !1,
      errorHandler: null,
      warnHandler: null,
      ignoredElements: [],
      keyCodes: Object.create(null),
      isReservedTag: I,
      isReservedAttr: I,
      isUnknownElement: I,
      getTagNamespace: M,
      parsePlatformTagName: D,
      mustUseProp: I,
      async: !0,
      _lifecycleHooks: U
    },
        z = /a-zA-Z\u00B7\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u037D\u037F-\u1FFF\u200C-\u200D\u203F-\u2040\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD/;

    function K(e) {
      var t = (e + "").charCodeAt(0);
      return 36 === t || 95 === t;
    }

    function J(e, t, n, r) {
      Object.defineProperty(e, t, {
        value: n,
        enumerable: !!r,
        writable: !0,
        configurable: !0
      });
    }

    var q = new RegExp("[^" + z.source + ".$_\\d]");
    var G,
        Z = ("__proto__" in {}),
        W = "undefined" != typeof window,
        X = "undefined" != typeof WXEnvironment && !!WXEnvironment.platform,
        Y = X && WXEnvironment.platform.toLowerCase(),
        Q = W && window.navigator.userAgent.toLowerCase(),
        ee = Q && /msie|trident/.test(Q),
        te = Q && Q.indexOf("msie 9.0") > 0,
        ne = Q && Q.indexOf("edge/") > 0,
        re = (Q && Q.indexOf("android"), Q && /iphone|ipad|ipod|ios/.test(Q) || "ios" === Y),
        ie = (Q && /chrome\/\d+/.test(Q), Q && /phantomjs/.test(Q), Q && Q.match(/firefox\/(\d+)/)),
        oe = {}.watch,
        ae = !1;
    if (W) try {
      var se = {};
      Object.defineProperty(se, "passive", {
        get: function get() {
          ae = !0;
        }
      }), window.addEventListener("test-passive", null, se);
    } catch (e) {}

    var ce = function ce() {
      return void 0 === G && (G = !W && !X && void 0 !== n.g && n.g.process && "server" === n.g.process.env.VUE_ENV), G;
    },
        ue = W && window.__VUE_DEVTOOLS_GLOBAL_HOOK__;

    function le(e) {
      return "function" == typeof e && /native code/.test(e.toString());
    }

    var fe,
        pe = "undefined" != typeof Symbol && le(Symbol) && "undefined" != typeof Reflect && le(Reflect.ownKeys);
    fe = "undefined" != typeof Set && le(Set) ? Set : function () {
      function e() {
        this.set = Object.create(null);
      }

      return e.prototype.has = function (e) {
        return !0 === this.set[e];
      }, e.prototype.add = function (e) {
        this.set[e] = !0;
      }, e.prototype.clear = function () {
        this.set = Object.create(null);
      }, e;
    }();

    var de = M,
        ve = 0,
        he = function he() {
      this.id = ve++, this.subs = [];
    };

    he.prototype.addSub = function (e) {
      this.subs.push(e);
    }, he.prototype.removeSub = function (e) {
      w(this.subs, e);
    }, he.prototype.depend = function () {
      he.target && he.target.addDep(this);
    }, he.prototype.notify = function () {
      var e = this.subs.slice();

      for (var t = 0, n = e.length; t < n; t++) {
        e[t].update();
      }
    }, he.target = null;
    var me = [];

    function ye(e) {
      me.push(e), he.target = e;
    }

    function ge() {
      me.pop(), he.target = me[me.length - 1];
    }

    var _e = function _e(e, t, n, r, i, o, a, s) {
      this.tag = e, this.data = t, this.children = n, this.text = r, this.elm = i, this.ns = void 0, this.context = o, this.fnContext = void 0, this.fnOptions = void 0, this.fnScopeId = void 0, this.key = t && t.key, this.componentOptions = a, this.componentInstance = void 0, this.parent = void 0, this.raw = !1, this.isStatic = !1, this.isRootInsert = !0, this.isComment = !1, this.isCloned = !1, this.isOnce = !1, this.asyncFactory = s, this.asyncMeta = void 0, this.isAsyncPlaceholder = !1;
    },
        be = {
      child: {
        configurable: !0
      }
    };

    be.child.get = function () {
      return this.componentInstance;
    }, Object.defineProperties(_e.prototype, be);

    var we = function we(e) {
      void 0 === e && (e = "");
      var t = new _e();
      return t.text = e, t.isComment = !0, t;
    };

    function xe(e) {
      return new _e(void 0, void 0, void 0, String(e));
    }

    function $e(e) {
      var t = new _e(e.tag, e.data, e.children && e.children.slice(), e.text, e.elm, e.context, e.componentOptions, e.asyncFactory);
      return t.ns = e.ns, t.isStatic = e.isStatic, t.key = e.key, t.isComment = e.isComment, t.fnContext = e.fnContext, t.fnOptions = e.fnOptions, t.fnScopeId = e.fnScopeId, t.asyncMeta = e.asyncMeta, t.isCloned = !0, t;
    }

    var Ce = Array.prototype,
        ke = Object.create(Ce);
    ["push", "pop", "shift", "unshift", "splice", "sort", "reverse"].forEach(function (e) {
      var t = Ce[e];
      J(ke, e, function () {
        for (var n = [], r = arguments.length; r--;) {
          n[r] = arguments[r];
        }

        var i,
            o = t.apply(this, n),
            a = this.__ob__;

        switch (e) {
          case "push":
          case "unshift":
            i = n;
            break;

          case "splice":
            i = n.slice(2);
        }

        return i && a.observeArray(i), a.dep.notify(), o;
      });
    });
    var Ae = Object.getOwnPropertyNames(ke),
        Oe = !0;

    function Se(e) {
      Oe = e;
    }

    var Te = function Te(e) {
      this.value = e, this.dep = new he(), this.vmCount = 0, J(e, "__ob__", this), Array.isArray(e) ? (Z ? function (e, t) {
        e.__proto__ = t;
      }(e, ke) : function (e, t, n) {
        for (var r = 0, i = n.length; r < i; r++) {
          var o = n[r];
          J(e, o, t[o]);
        }
      }(e, ke, Ae), this.observeArray(e)) : this.walk(e);
    };

    function Ee(e, t) {
      var n;
      if (l(e) && !(e instanceof _e)) return $(e, "__ob__") && e.__ob__ instanceof Te ? n = e.__ob__ : Oe && !ce() && (Array.isArray(e) || p(e)) && Object.isExtensible(e) && !e._isVue && (n = new Te(e)), t && n && n.vmCount++, n;
    }

    function je(e, t, n, r, i) {
      var o = new he(),
          a = Object.getOwnPropertyDescriptor(e, t);

      if (!a || !1 !== a.configurable) {
        var s = a && a.get,
            c = a && a.set;
        s && !c || 2 !== arguments.length || (n = e[t]);
        var u = !i && Ee(n);
        Object.defineProperty(e, t, {
          enumerable: !0,
          configurable: !0,
          get: function get() {
            var t = s ? s.call(e) : n;
            return he.target && (o.depend(), u && (u.dep.depend(), Array.isArray(t) && Me(t))), t;
          },
          set: function set(t) {
            var r = s ? s.call(e) : n;
            t === r || t != t && r != r || s && !c || (c ? c.call(e, t) : n = t, u = !i && Ee(t), o.notify());
          }
        });
      }
    }

    function Le(e, t, n) {
      if (Array.isArray(e) && v(t)) return e.length = Math.max(e.length, t), e.splice(t, 1, n), n;
      if (t in e && !(t in Object.prototype)) return e[t] = n, n;
      var r = e.__ob__;
      return e._isVue || r && r.vmCount ? n : r ? (je(r.value, t, n), r.dep.notify(), n) : (e[t] = n, n);
    }

    function Ne(e, t) {
      if (Array.isArray(e) && v(t)) e.splice(t, 1);else {
        var n = e.__ob__;
        e._isVue || n && n.vmCount || $(e, t) && (delete e[t], n && n.dep.notify());
      }
    }

    function Me(e) {
      for (var t = void 0, n = 0, r = e.length; n < r; n++) {
        (t = e[n]) && t.__ob__ && t.__ob__.dep.depend(), Array.isArray(t) && Me(t);
      }
    }

    Te.prototype.walk = function (e) {
      for (var t = Object.keys(e), n = 0; n < t.length; n++) {
        je(e, t[n]);
      }
    }, Te.prototype.observeArray = function (e) {
      for (var t = 0, n = e.length; t < n; t++) {
        Ee(e[t]);
      }
    };
    var Ie = V.optionMergeStrategies;

    function De(e, t) {
      if (!t) return e;

      for (var n, r, i, o = pe ? Reflect.ownKeys(t) : Object.keys(t), a = 0; a < o.length; a++) {
        "__ob__" !== (n = o[a]) && (r = e[n], i = t[n], $(e, n) ? r !== i && p(r) && p(i) && De(r, i) : Le(e, n, i));
      }

      return e;
    }

    function Pe(e, t, n) {
      return n ? function () {
        var r = "function" == typeof t ? t.call(n, n) : t,
            i = "function" == typeof e ? e.call(n, n) : e;
        return r ? De(r, i) : i;
      } : t ? e ? function () {
        return De("function" == typeof t ? t.call(this, this) : t, "function" == typeof e ? e.call(this, this) : e);
      } : t : e;
    }

    function Fe(e, t) {
      var n = t ? e ? e.concat(t) : Array.isArray(t) ? t : [t] : e;
      return n ? function (e) {
        for (var t = [], n = 0; n < e.length; n++) {
          -1 === t.indexOf(e[n]) && t.push(e[n]);
        }

        return t;
      }(n) : n;
    }

    function Re(e, t, n, r) {
      var i = Object.create(e || null);
      return t ? L(i, t) : i;
    }

    Ie.data = function (e, t, n) {
      return n ? Pe(e, t, n) : t && "function" != typeof t ? e : Pe(e, t);
    }, U.forEach(function (e) {
      Ie[e] = Fe;
    }), B.forEach(function (e) {
      Ie[e + "s"] = Re;
    }), Ie.watch = function (e, t, n, r) {
      if (e === oe && (e = void 0), t === oe && (t = void 0), !t) return Object.create(e || null);
      if (!e) return t;
      var i = {};

      for (var o in L(i, e), t) {
        var a = i[o],
            s = t[o];
        a && !Array.isArray(a) && (a = [a]), i[o] = a ? a.concat(s) : Array.isArray(s) ? s : [s];
      }

      return i;
    }, Ie.props = Ie.methods = Ie.inject = Ie.computed = function (e, t, n, r) {
      if (!e) return t;
      var i = Object.create(null);
      return L(i, e), t && L(i, t), i;
    }, Ie.provide = Pe;

    var He = function He(e, t) {
      return void 0 === t ? e : t;
    };

    function Be(e, t, n) {
      if ("function" == typeof t && (t = t.options), function (e, t) {
        var n = e.props;

        if (n) {
          var r,
              i,
              o = {};
          if (Array.isArray(n)) for (r = n.length; r--;) {
            "string" == typeof (i = n[r]) && (o[A(i)] = {
              type: null
            });
          } else if (p(n)) for (var a in n) {
            i = n[a], o[A(a)] = p(i) ? i : {
              type: i
            };
          }
          e.props = o;
        }
      }(t), function (e, t) {
        var n = e.inject;

        if (n) {
          var r = e.inject = {};
          if (Array.isArray(n)) for (var i = 0; i < n.length; i++) {
            r[n[i]] = {
              from: n[i]
            };
          } else if (p(n)) for (var o in n) {
            var a = n[o];
            r[o] = p(a) ? L({
              from: o
            }, a) : {
              from: a
            };
          }
        }
      }(t), function (e) {
        var t = e.directives;
        if (t) for (var n in t) {
          var r = t[n];
          "function" == typeof r && (t[n] = {
            bind: r,
            update: r
          });
        }
      }(t), !t._base && (t["extends"] && (e = Be(e, t["extends"], n)), t.mixins)) for (var r = 0, i = t.mixins.length; r < i; r++) {
        e = Be(e, t.mixins[r], n);
      }
      var o,
          a = {};

      for (o in e) {
        s(o);
      }

      for (o in t) {
        $(e, o) || s(o);
      }

      function s(r) {
        var i = Ie[r] || He;
        a[r] = i(e[r], t[r], n, r);
      }

      return a;
    }

    function Ue(e, t, n, r) {
      if ("string" == typeof n) {
        var i = e[t];
        if ($(i, n)) return i[n];
        var o = A(n);
        if ($(i, o)) return i[o];
        var a = O(o);
        return $(i, a) ? i[a] : i[n] || i[o] || i[a];
      }
    }

    function Ve(e, t, n, r) {
      var i = t[e],
          o = !$(n, e),
          a = n[e],
          s = qe(Boolean, i.type);
      if (s > -1) if (o && !$(i, "default")) a = !1;else if ("" === a || a === T(e)) {
        var c = qe(String, i.type);
        (c < 0 || s < c) && (a = !0);
      }

      if (void 0 === a) {
        a = function (e, t, n) {
          if (!$(t, "default")) return;
          var r = t["default"];
          0;
          if (e && e.$options.propsData && void 0 === e.$options.propsData[n] && void 0 !== e._props[n]) return e._props[n];
          return "function" == typeof r && "Function" !== Ke(t.type) ? r.call(e) : r;
        }(r, i, e);

        var u = Oe;
        Se(!0), Ee(a), Se(u);
      }

      return a;
    }

    var ze = /^\s*function (\w+)/;

    function Ke(e) {
      var t = e && e.toString().match(ze);
      return t ? t[1] : "";
    }

    function Je(e, t) {
      return Ke(e) === Ke(t);
    }

    function qe(e, t) {
      if (!Array.isArray(t)) return Je(t, e) ? 0 : -1;

      for (var n = 0, r = t.length; n < r; n++) {
        if (Je(t[n], e)) return n;
      }

      return -1;
    }

    function Ge(e, t, n) {
      ye();

      try {
        if (t) for (var r = t; r = r.$parent;) {
          var i = r.$options.errorCaptured;
          if (i) for (var o = 0; o < i.length; o++) {
            try {
              if (!1 === i[o].call(r, e, t, n)) return;
            } catch (e) {
              We(e, r, "errorCaptured hook");
            }
          }
        }
        We(e, t, n);
      } finally {
        ge();
      }
    }

    function Ze(e, t, n, r, i) {
      var o;

      try {
        (o = n ? e.apply(t, n) : e.call(t)) && !o._isVue && h(o) && !o._handled && (o["catch"](function (e) {
          return Ge(e, r, i + " (Promise/async)");
        }), o._handled = !0);
      } catch (e) {
        Ge(e, r, i);
      }

      return o;
    }

    function We(e, t, n) {
      if (V.errorHandler) try {
        return V.errorHandler.call(null, e, t, n);
      } catch (t) {
        t !== e && Xe(t, null, "config.errorHandler");
      }
      Xe(e, t, n);
    }

    function Xe(e, t, n) {
      if (!W && !X || "undefined" == typeof console) throw e;
      console.error(e);
    }

    var Ye,
        Qe = !1,
        et = [],
        tt = !1;

    function nt() {
      tt = !1;
      var e = et.slice(0);
      et.length = 0;

      for (var t = 0; t < e.length; t++) {
        e[t]();
      }
    }

    if ("undefined" != typeof Promise && le(Promise)) {
      var rt = Promise.resolve();
      Ye = function Ye() {
        rt.then(nt), re && setTimeout(M);
      }, Qe = !0;
    } else if (ee || "undefined" == typeof MutationObserver || !le(MutationObserver) && "[object MutationObserverConstructor]" !== MutationObserver.toString()) Ye = "undefined" != typeof setImmediate && le(setImmediate) ? function () {
      setImmediate(nt);
    } : function () {
      setTimeout(nt, 0);
    };else {
      var it = 1,
          ot = new MutationObserver(nt),
          at = document.createTextNode(String(it));
      ot.observe(at, {
        characterData: !0
      }), Ye = function Ye() {
        it = (it + 1) % 2, at.data = String(it);
      }, Qe = !0;
    }

    function st(e, t) {
      var n;
      if (et.push(function () {
        if (e) try {
          e.call(t);
        } catch (e) {
          Ge(e, t, "nextTick");
        } else n && n(t);
      }), tt || (tt = !0, Ye()), !e && "undefined" != typeof Promise) return new Promise(function (e) {
        n = e;
      });
    }

    var ct = new fe();

    function ut(e) {
      lt(e, ct), ct.clear();
    }

    function lt(e, t) {
      var n,
          r,
          i = Array.isArray(e);

      if (!(!i && !l(e) || Object.isFrozen(e) || e instanceof _e)) {
        if (e.__ob__) {
          var o = e.__ob__.dep.id;
          if (t.has(o)) return;
          t.add(o);
        }

        if (i) for (n = e.length; n--;) {
          lt(e[n], t);
        } else for (n = (r = Object.keys(e)).length; n--;) {
          lt(e[r[n]], t);
        }
      }
    }

    var ft = C(function (e) {
      var t = "&" === e.charAt(0),
          n = "~" === (e = t ? e.slice(1) : e).charAt(0),
          r = "!" === (e = n ? e.slice(1) : e).charAt(0);
      return {
        name: e = r ? e.slice(1) : e,
        once: n,
        capture: r,
        passive: t
      };
    });

    function pt(e, t) {
      function n() {
        var e = arguments,
            r = n.fns;
        if (!Array.isArray(r)) return Ze(r, null, arguments, t, "v-on handler");

        for (var i = r.slice(), o = 0; o < i.length; o++) {
          Ze(i[o], null, e, t, "v-on handler");
        }
      }

      return n.fns = e, n;
    }

    function dt(e, t, n, r, i, o) {
      var s, u, l, f;

      for (s in e) {
        u = e[s], l = t[s], f = ft(s), a(u) || (a(l) ? (a(u.fns) && (u = e[s] = pt(u, o)), c(f.once) && (u = e[s] = i(f.name, u, f.capture)), n(f.name, u, f.capture, f.passive, f.params)) : u !== l && (l.fns = u, e[s] = l));
      }

      for (s in t) {
        a(e[s]) && r((f = ft(s)).name, t[s], f.capture);
      }
    }

    function vt(e, t, n) {
      var r;
      e instanceof _e && (e = e.data.hook || (e.data.hook = {}));
      var i = e[t];

      function o() {
        n.apply(this, arguments), w(r.fns, o);
      }

      a(i) ? r = pt([o]) : s(i.fns) && c(i.merged) ? (r = i).fns.push(o) : r = pt([i, o]), r.merged = !0, e[t] = r;
    }

    function ht(e, t, n, r, i) {
      if (s(t)) {
        if ($(t, n)) return e[n] = t[n], i || delete t[n], !0;
        if ($(t, r)) return e[n] = t[r], i || delete t[r], !0;
      }

      return !1;
    }

    function mt(e) {
      return u(e) ? [xe(e)] : Array.isArray(e) ? gt(e) : void 0;
    }

    function yt(e) {
      return s(e) && s(e.text) && !1 === e.isComment;
    }

    function gt(e, t) {
      var n,
          r,
          i,
          o,
          l = [];

      for (n = 0; n < e.length; n++) {
        a(r = e[n]) || "boolean" == typeof r || (o = l[i = l.length - 1], Array.isArray(r) ? r.length > 0 && (yt((r = gt(r, (t || "") + "_" + n))[0]) && yt(o) && (l[i] = xe(o.text + r[0].text), r.shift()), l.push.apply(l, r)) : u(r) ? yt(o) ? l[i] = xe(o.text + r) : "" !== r && l.push(xe(r)) : yt(r) && yt(o) ? l[i] = xe(o.text + r.text) : (c(e._isVList) && s(r.tag) && a(r.key) && s(t) && (r.key = "__vlist" + t + "_" + n + "__"), l.push(r)));
      }

      return l;
    }

    function _t(e, t) {
      if (e) {
        for (var n = Object.create(null), r = pe ? Reflect.ownKeys(e) : Object.keys(e), i = 0; i < r.length; i++) {
          var o = r[i];

          if ("__ob__" !== o) {
            for (var a = e[o].from, s = t; s;) {
              if (s._provided && $(s._provided, a)) {
                n[o] = s._provided[a];
                break;
              }

              s = s.$parent;
            }

            if (!s) if ("default" in e[o]) {
              var c = e[o]["default"];
              n[o] = "function" == typeof c ? c.call(t) : c;
            } else 0;
          }
        }

        return n;
      }
    }

    function bt(e, t) {
      if (!e || !e.length) return {};

      for (var n = {}, r = 0, i = e.length; r < i; r++) {
        var o = e[r],
            a = o.data;
        if (a && a.attrs && a.attrs.slot && delete a.attrs.slot, o.context !== t && o.fnContext !== t || !a || null == a.slot) (n["default"] || (n["default"] = [])).push(o);else {
          var s = a.slot,
              c = n[s] || (n[s] = []);
          "template" === o.tag ? c.push.apply(c, o.children || []) : c.push(o);
        }
      }

      for (var u in n) {
        n[u].every(wt) && delete n[u];
      }

      return n;
    }

    function wt(e) {
      return e.isComment && !e.asyncFactory || " " === e.text;
    }

    function xt(e) {
      return e.isComment && e.asyncFactory;
    }

    function $t(e, t, n) {
      var r,
          i = Object.keys(t).length > 0,
          a = e ? !!e.$stable : !i,
          s = e && e.$key;

      if (e) {
        if (e._normalized) return e._normalized;
        if (a && n && n !== o && s === n.$key && !i && !n.$hasNormal) return n;

        for (var c in r = {}, e) {
          e[c] && "$" !== c[0] && (r[c] = Ct(t, c, e[c]));
        }
      } else r = {};

      for (var u in t) {
        u in r || (r[u] = kt(t, u));
      }

      return e && Object.isExtensible(e) && (e._normalized = r), J(r, "$stable", a), J(r, "$key", s), J(r, "$hasNormal", i), r;
    }

    function Ct(e, t, n) {
      var r = function r() {
        var e = arguments.length ? n.apply(null, arguments) : n({}),
            t = (e = e && "object" == _typeof(e) && !Array.isArray(e) ? [e] : mt(e)) && e[0];
        return e && (!t || 1 === e.length && t.isComment && !xt(t)) ? void 0 : e;
      };

      return n.proxy && Object.defineProperty(e, t, {
        get: r,
        enumerable: !0,
        configurable: !0
      }), r;
    }

    function kt(e, t) {
      return function () {
        return e[t];
      };
    }

    function At(e, t) {
      var n, r, i, o, a;
      if (Array.isArray(e) || "string" == typeof e) for (n = new Array(e.length), r = 0, i = e.length; r < i; r++) {
        n[r] = t(e[r], r);
      } else if ("number" == typeof e) for (n = new Array(e), r = 0; r < e; r++) {
        n[r] = t(r + 1, r);
      } else if (l(e)) if (pe && e[Symbol.iterator]) {
        n = [];

        for (var c = e[Symbol.iterator](), u = c.next(); !u.done;) {
          n.push(t(u.value, n.length)), u = c.next();
        }
      } else for (o = Object.keys(e), n = new Array(o.length), r = 0, i = o.length; r < i; r++) {
        a = o[r], n[r] = t(e[a], a, r);
      }
      return s(n) || (n = []), n._isVList = !0, n;
    }

    function Ot(e, t, n, r) {
      var i,
          o = this.$scopedSlots[e];
      o ? (n = n || {}, r && (n = L(L({}, r), n)), i = o(n) || ("function" == typeof t ? t() : t)) : i = this.$slots[e] || ("function" == typeof t ? t() : t);
      var a = n && n.slot;
      return a ? this.$createElement("template", {
        slot: a
      }, i) : i;
    }

    function St(e) {
      return Ue(this.$options, "filters", e) || D;
    }

    function Tt(e, t) {
      return Array.isArray(e) ? -1 === e.indexOf(t) : e !== t;
    }

    function Et(e, t, n, r, i) {
      var o = V.keyCodes[t] || n;
      return i && r && !V.keyCodes[t] ? Tt(i, r) : o ? Tt(o, e) : r ? T(r) !== t : void 0 === e;
    }

    function jt(e, t, n, r, i) {
      if (n) if (l(n)) {
        var o;
        Array.isArray(n) && (n = N(n));

        var a = function a(_a2) {
          if ("class" === _a2 || "style" === _a2 || b(_a2)) o = e;else {
            var s = e.attrs && e.attrs.type;
            o = r || V.mustUseProp(t, s, _a2) ? e.domProps || (e.domProps = {}) : e.attrs || (e.attrs = {});
          }
          var c = A(_a2),
              u = T(_a2);
          c in o || u in o || (o[_a2] = n[_a2], i && ((e.on || (e.on = {}))["update:" + _a2] = function (e) {
            n[_a2] = e;
          }));
        };

        for (var s in n) {
          a(s);
        }
      } else ;
      return e;
    }

    function Lt(e, t) {
      var n = this._staticTrees || (this._staticTrees = []),
          r = n[e];
      return r && !t || Mt(r = n[e] = this.$options.staticRenderFns[e].call(this._renderProxy, null, this), "__static__" + e, !1), r;
    }

    function Nt(e, t, n) {
      return Mt(e, "__once__" + t + (n ? "_" + n : ""), !0), e;
    }

    function Mt(e, t, n) {
      if (Array.isArray(e)) for (var r = 0; r < e.length; r++) {
        e[r] && "string" != typeof e[r] && It(e[r], t + "_" + r, n);
      } else It(e, t, n);
    }

    function It(e, t, n) {
      e.isStatic = !0, e.key = t, e.isOnce = n;
    }

    function Dt(e, t) {
      if (t) if (p(t)) {
        var n = e.on = e.on ? L({}, e.on) : {};

        for (var r in t) {
          var i = n[r],
              o = t[r];
          n[r] = i ? [].concat(i, o) : o;
        }
      } else ;
      return e;
    }

    function Pt(e, t, n, r) {
      t = t || {
        $stable: !n
      };

      for (var i = 0; i < e.length; i++) {
        var o = e[i];
        Array.isArray(o) ? Pt(o, t, n) : o && (o.proxy && (o.fn.proxy = !0), t[o.key] = o.fn);
      }

      return r && (t.$key = r), t;
    }

    function Ft(e, t) {
      for (var n = 0; n < t.length; n += 2) {
        var r = t[n];
        "string" == typeof r && r && (e[t[n]] = t[n + 1]);
      }

      return e;
    }

    function Rt(e, t) {
      return "string" == typeof e ? t + e : e;
    }

    function Ht(e) {
      e._o = Nt, e._n = y, e._s = m, e._l = At, e._t = Ot, e._q = P, e._i = F, e._m = Lt, e._f = St, e._k = Et, e._b = jt, e._v = xe, e._e = we, e._u = Pt, e._g = Dt, e._d = Ft, e._p = Rt;
    }

    function Bt(e, t, n, r, i) {
      var a,
          s = this,
          u = i.options;
      $(r, "_uid") ? (a = Object.create(r))._original = r : (a = r, r = r._original);
      var l = c(u._compiled),
          f = !l;
      this.data = e, this.props = t, this.children = n, this.parent = r, this.listeners = e.on || o, this.injections = _t(u.inject, r), this.slots = function () {
        return s.$slots || $t(e.scopedSlots, s.$slots = bt(n, r)), s.$slots;
      }, Object.defineProperty(this, "scopedSlots", {
        enumerable: !0,
        get: function get() {
          return $t(e.scopedSlots, this.slots());
        }
      }), l && (this.$options = u, this.$slots = this.slots(), this.$scopedSlots = $t(e.scopedSlots, this.$slots)), u._scopeId ? this._c = function (e, t, n, i) {
        var o = Gt(a, e, t, n, i, f);
        return o && !Array.isArray(o) && (o.fnScopeId = u._scopeId, o.fnContext = r), o;
      } : this._c = function (e, t, n, r) {
        return Gt(a, e, t, n, r, f);
      };
    }

    function Ut(e, t, n, r, i) {
      var o = $e(e);
      return o.fnContext = n, o.fnOptions = r, t.slot && ((o.data || (o.data = {})).slot = t.slot), o;
    }

    function Vt(e, t) {
      for (var n in t) {
        e[A(n)] = t[n];
      }
    }

    Ht(Bt.prototype);
    var zt = {
      init: function init(e, t) {
        if (e.componentInstance && !e.componentInstance._isDestroyed && e.data.keepAlive) {
          var n = e;
          zt.prepatch(n, n);
        } else {
          var r = e.componentInstance = function (e, t) {
            var n = {
              _isComponent: !0,
              _parentVnode: e,
              parent: t
            },
                r = e.data.inlineTemplate;
            s(r) && (n.render = r.render, n.staticRenderFns = r.staticRenderFns);
            return new e.componentOptions.Ctor(n);
          }(e, on);

          r.$mount(t ? e.elm : void 0, t);
        }
      },
      prepatch: function prepatch(e, t) {
        var n = t.componentOptions;
        !function (e, t, n, r, i) {
          0;
          var a = r.data.scopedSlots,
              s = e.$scopedSlots,
              c = !!(a && !a.$stable || s !== o && !s.$stable || a && e.$scopedSlots.$key !== a.$key || !a && e.$scopedSlots.$key),
              u = !!(i || e.$options._renderChildren || c);
          e.$options._parentVnode = r, e.$vnode = r, e._vnode && (e._vnode.parent = r);

          if (e.$options._renderChildren = i, e.$attrs = r.data.attrs || o, e.$listeners = n || o, t && e.$options.props) {
            Se(!1);

            for (var l = e._props, f = e.$options._propKeys || [], p = 0; p < f.length; p++) {
              var d = f[p],
                  v = e.$options.props;
              l[d] = Ve(d, v, t, e);
            }

            Se(!0), e.$options.propsData = t;
          }

          n = n || o;
          var h = e.$options._parentListeners;
          e.$options._parentListeners = n, rn(e, n, h), u && (e.$slots = bt(i, r.context), e.$forceUpdate());
          0;
        }(t.componentInstance = e.componentInstance, n.propsData, n.listeners, t, n.children);
      },
      insert: function insert(e) {
        var t,
            n = e.context,
            r = e.componentInstance;
        r._isMounted || (r._isMounted = !0, ln(r, "mounted")), e.data.keepAlive && (n._isMounted ? ((t = r)._inactive = !1, pn.push(t)) : cn(r, !0));
      },
      destroy: function destroy(e) {
        var t = e.componentInstance;
        t._isDestroyed || (e.data.keepAlive ? un(t, !0) : t.$destroy());
      }
    },
        Kt = Object.keys(zt);

    function Jt(e, t, n, r, i) {
      if (!a(e)) {
        var u = n.$options._base;

        if (l(e) && (e = u.extend(e)), "function" == typeof e) {
          var f;
          if (a(e.cid) && (e = function (e, t) {
            if (c(e.error) && s(e.errorComp)) return e.errorComp;
            if (s(e.resolved)) return e.resolved;
            var n = Xt;
            n && s(e.owners) && -1 === e.owners.indexOf(n) && e.owners.push(n);
            if (c(e.loading) && s(e.loadingComp)) return e.loadingComp;

            if (n && !s(e.owners)) {
              var r = e.owners = [n],
                  i = !0,
                  o = null,
                  u = null;
              n.$on("hook:destroyed", function () {
                return w(r, n);
              });

              var f = function f(e) {
                for (var t = 0, n = r.length; t < n; t++) {
                  r[t].$forceUpdate();
                }

                e && (r.length = 0, null !== o && (clearTimeout(o), o = null), null !== u && (clearTimeout(u), u = null));
              },
                  p = R(function (n) {
                e.resolved = Yt(n, t), i ? r.length = 0 : f(!0);
              }),
                  d = R(function (t) {
                s(e.errorComp) && (e.error = !0, f(!0));
              }),
                  v = e(p, d);

              return l(v) && (h(v) ? a(e.resolved) && v.then(p, d) : h(v.component) && (v.component.then(p, d), s(v.error) && (e.errorComp = Yt(v.error, t)), s(v.loading) && (e.loadingComp = Yt(v.loading, t), 0 === v.delay ? e.loading = !0 : o = setTimeout(function () {
                o = null, a(e.resolved) && a(e.error) && (e.loading = !0, f(!1));
              }, v.delay || 200)), s(v.timeout) && (u = setTimeout(function () {
                u = null, a(e.resolved) && d(null);
              }, v.timeout)))), i = !1, e.loading ? e.loadingComp : e.resolved;
            }
          }(f = e, u), void 0 === e)) return function (e, t, n, r, i) {
            var o = we();
            return o.asyncFactory = e, o.asyncMeta = {
              data: t,
              context: n,
              children: r,
              tag: i
            }, o;
          }(f, t, n, r, i);
          t = t || {}, Ln(e), s(t.model) && function (e, t) {
            var n = e.model && e.model.prop || "value",
                r = e.model && e.model.event || "input";
            (t.attrs || (t.attrs = {}))[n] = t.model.value;
            var i = t.on || (t.on = {}),
                o = i[r],
                a = t.model.callback;
            s(o) ? (Array.isArray(o) ? -1 === o.indexOf(a) : o !== a) && (i[r] = [a].concat(o)) : i[r] = a;
          }(e.options, t);

          var p = function (e, t, n) {
            var r = t.options.props;

            if (!a(r)) {
              var i = {},
                  o = e.attrs,
                  c = e.props;
              if (s(o) || s(c)) for (var u in r) {
                var l = T(u);
                ht(i, c, u, l, !0) || ht(i, o, u, l, !1);
              }
              return i;
            }
          }(t, e);

          if (c(e.options.functional)) return function (e, t, n, r, i) {
            var a = e.options,
                c = {},
                u = a.props;
            if (s(u)) for (var l in u) {
              c[l] = Ve(l, u, t || o);
            } else s(n.attrs) && Vt(c, n.attrs), s(n.props) && Vt(c, n.props);
            var f = new Bt(n, c, i, r, e),
                p = a.render.call(null, f._c, f);
            if (p instanceof _e) return Ut(p, n, f.parent, a);

            if (Array.isArray(p)) {
              for (var d = mt(p) || [], v = new Array(d.length), h = 0; h < d.length; h++) {
                v[h] = Ut(d[h], n, f.parent, a);
              }

              return v;
            }
          }(e, p, t, n, r);
          var d = t.on;

          if (t.on = t.nativeOn, c(e.options["abstract"])) {
            var v = t.slot;
            t = {}, v && (t.slot = v);
          }

          !function (e) {
            for (var t = e.hook || (e.hook = {}), n = 0; n < Kt.length; n++) {
              var r = Kt[n],
                  i = t[r],
                  o = zt[r];
              i === o || i && i._merged || (t[r] = i ? qt(o, i) : o);
            }
          }(t);
          var m = e.options.name || i;
          return new _e("vue-component-" + e.cid + (m ? "-" + m : ""), t, void 0, void 0, void 0, n, {
            Ctor: e,
            propsData: p,
            listeners: d,
            tag: i,
            children: r
          }, f);
        }
      }
    }

    function qt(e, t) {
      var n = function n(_n2, r) {
        e(_n2, r), t(_n2, r);
      };

      return n._merged = !0, n;
    }

    function Gt(e, t, n, r, i, o) {
      return (Array.isArray(n) || u(n)) && (i = r, r = n, n = void 0), c(o) && (i = 2), function (e, t, n, r, i) {
        if (s(n) && s(n.__ob__)) return we();
        s(n) && s(n.is) && (t = n.is);
        if (!t) return we();
        0;
        Array.isArray(r) && "function" == typeof r[0] && ((n = n || {}).scopedSlots = {
          "default": r[0]
        }, r.length = 0);
        2 === i ? r = mt(r) : 1 === i && (r = function (e) {
          for (var t = 0; t < e.length; t++) {
            if (Array.isArray(e[t])) return Array.prototype.concat.apply([], e);
          }

          return e;
        }(r));
        var o, a;

        if ("string" == typeof t) {
          var c;
          a = e.$vnode && e.$vnode.ns || V.getTagNamespace(t), o = V.isReservedTag(t) ? new _e(V.parsePlatformTagName(t), n, r, void 0, void 0, e) : n && n.pre || !s(c = Ue(e.$options, "components", t)) ? new _e(t, n, r, void 0, void 0, e) : Jt(c, n, e, r, t);
        } else o = Jt(t, n, e, r);

        return Array.isArray(o) ? o : s(o) ? (s(a) && Zt(o, a), s(n) && function (e) {
          l(e.style) && ut(e.style);
          l(e["class"]) && ut(e["class"]);
        }(n), o) : we();
      }(e, t, n, r, i);
    }

    function Zt(e, t, n) {
      if (e.ns = t, "foreignObject" === e.tag && (t = void 0, n = !0), s(e.children)) for (var r = 0, i = e.children.length; r < i; r++) {
        var o = e.children[r];
        s(o.tag) && (a(o.ns) || c(n) && "svg" !== o.tag) && Zt(o, t, n);
      }
    }

    var Wt,
        Xt = null;

    function Yt(e, t) {
      return (e.__esModule || pe && "Module" === e[Symbol.toStringTag]) && (e = e["default"]), l(e) ? t.extend(e) : e;
    }

    function Qt(e) {
      if (Array.isArray(e)) for (var t = 0; t < e.length; t++) {
        var n = e[t];
        if (s(n) && (s(n.componentOptions) || xt(n))) return n;
      }
    }

    function en(e, t) {
      Wt.$on(e, t);
    }

    function tn(e, t) {
      Wt.$off(e, t);
    }

    function nn(e, t) {
      var n = Wt;
      return function r() {
        var i = t.apply(null, arguments);
        null !== i && n.$off(e, r);
      };
    }

    function rn(e, t, n) {
      Wt = e, dt(t, n || {}, en, tn, nn, e), Wt = void 0;
    }

    var on = null;

    function an(e) {
      var t = on;
      return on = e, function () {
        on = t;
      };
    }

    function sn(e) {
      for (; e && (e = e.$parent);) {
        if (e._inactive) return !0;
      }

      return !1;
    }

    function cn(e, t) {
      if (t) {
        if (e._directInactive = !1, sn(e)) return;
      } else if (e._directInactive) return;

      if (e._inactive || null === e._inactive) {
        e._inactive = !1;

        for (var n = 0; n < e.$children.length; n++) {
          cn(e.$children[n]);
        }

        ln(e, "activated");
      }
    }

    function un(e, t) {
      if (!(t && (e._directInactive = !0, sn(e)) || e._inactive)) {
        e._inactive = !0;

        for (var n = 0; n < e.$children.length; n++) {
          un(e.$children[n]);
        }

        ln(e, "deactivated");
      }
    }

    function ln(e, t) {
      ye();
      var n = e.$options[t],
          r = t + " hook";
      if (n) for (var i = 0, o = n.length; i < o; i++) {
        Ze(n[i], e, null, e, r);
      }
      e._hasHookEvent && e.$emit("hook:" + t), ge();
    }

    var fn = [],
        pn = [],
        dn = {},
        vn = !1,
        hn = !1,
        mn = 0;
    var yn = 0,
        gn = Date.now;

    if (W && !ee) {
      var _n = window.performance;
      _n && "function" == typeof _n.now && gn() > document.createEvent("Event").timeStamp && (gn = function gn() {
        return _n.now();
      });
    }

    function bn() {
      var e, t;

      for (yn = gn(), hn = !0, fn.sort(function (e, t) {
        return e.id - t.id;
      }), mn = 0; mn < fn.length; mn++) {
        (e = fn[mn]).before && e.before(), t = e.id, dn[t] = null, e.run();
      }

      var n = pn.slice(),
          r = fn.slice();
      mn = fn.length = pn.length = 0, dn = {}, vn = hn = !1, function (e) {
        for (var t = 0; t < e.length; t++) {
          e[t]._inactive = !0, cn(e[t], !0);
        }
      }(n), function (e) {
        var t = e.length;

        for (; t--;) {
          var n = e[t],
              r = n.vm;
          r._watcher === n && r._isMounted && !r._isDestroyed && ln(r, "updated");
        }
      }(r), ue && V.devtools && ue.emit("flush");
    }

    var wn = 0,
        xn = function xn(e, t, n, r, i) {
      this.vm = e, i && (e._watcher = this), e._watchers.push(this), r ? (this.deep = !!r.deep, this.user = !!r.user, this.lazy = !!r.lazy, this.sync = !!r.sync, this.before = r.before) : this.deep = this.user = this.lazy = this.sync = !1, this.cb = n, this.id = ++wn, this.active = !0, this.dirty = this.lazy, this.deps = [], this.newDeps = [], this.depIds = new fe(), this.newDepIds = new fe(), this.expression = "", "function" == typeof t ? this.getter = t : (this.getter = function (e) {
        if (!q.test(e)) {
          var t = e.split(".");
          return function (e) {
            for (var n = 0; n < t.length; n++) {
              if (!e) return;
              e = e[t[n]];
            }

            return e;
          };
        }
      }(t), this.getter || (this.getter = M)), this.value = this.lazy ? void 0 : this.get();
    };

    xn.prototype.get = function () {
      var e;
      ye(this);
      var t = this.vm;

      try {
        e = this.getter.call(t, t);
      } catch (e) {
        if (!this.user) throw e;
        Ge(e, t, 'getter for watcher "' + this.expression + '"');
      } finally {
        this.deep && ut(e), ge(), this.cleanupDeps();
      }

      return e;
    }, xn.prototype.addDep = function (e) {
      var t = e.id;
      this.newDepIds.has(t) || (this.newDepIds.add(t), this.newDeps.push(e), this.depIds.has(t) || e.addSub(this));
    }, xn.prototype.cleanupDeps = function () {
      for (var e = this.deps.length; e--;) {
        var t = this.deps[e];
        this.newDepIds.has(t.id) || t.removeSub(this);
      }

      var n = this.depIds;
      this.depIds = this.newDepIds, this.newDepIds = n, this.newDepIds.clear(), n = this.deps, this.deps = this.newDeps, this.newDeps = n, this.newDeps.length = 0;
    }, xn.prototype.update = function () {
      this.lazy ? this.dirty = !0 : this.sync ? this.run() : function (e) {
        var t = e.id;

        if (null == dn[t]) {
          if (dn[t] = !0, hn) {
            for (var n = fn.length - 1; n > mn && fn[n].id > e.id;) {
              n--;
            }

            fn.splice(n + 1, 0, e);
          } else fn.push(e);

          vn || (vn = !0, st(bn));
        }
      }(this);
    }, xn.prototype.run = function () {
      if (this.active) {
        var e = this.get();

        if (e !== this.value || l(e) || this.deep) {
          var t = this.value;

          if (this.value = e, this.user) {
            var n = 'callback for watcher "' + this.expression + '"';
            Ze(this.cb, this.vm, [e, t], this.vm, n);
          } else this.cb.call(this.vm, e, t);
        }
      }
    }, xn.prototype.evaluate = function () {
      this.value = this.get(), this.dirty = !1;
    }, xn.prototype.depend = function () {
      for (var e = this.deps.length; e--;) {
        this.deps[e].depend();
      }
    }, xn.prototype.teardown = function () {
      if (this.active) {
        this.vm._isBeingDestroyed || w(this.vm._watchers, this);

        for (var e = this.deps.length; e--;) {
          this.deps[e].removeSub(this);
        }

        this.active = !1;
      }
    };
    var $n = {
      enumerable: !0,
      configurable: !0,
      get: M,
      set: M
    };

    function Cn(e, t, n) {
      $n.get = function () {
        return this[t][n];
      }, $n.set = function (e) {
        this[t][n] = e;
      }, Object.defineProperty(e, n, $n);
    }

    function kn(e) {
      e._watchers = [];
      var t = e.$options;
      t.props && function (e, t) {
        var n = e.$options.propsData || {},
            r = e._props = {},
            i = e.$options._propKeys = [];
        e.$parent && Se(!1);

        var o = function o(_o2) {
          i.push(_o2);
          var a = Ve(_o2, t, n, e);
          je(r, _o2, a), _o2 in e || Cn(e, "_props", _o2);
        };

        for (var a in t) {
          o(a);
        }

        Se(!0);
      }(e, t.props), t.methods && function (e, t) {
        e.$options.props;

        for (var n in t) {
          e[n] = "function" != typeof t[n] ? M : E(t[n], e);
        }
      }(e, t.methods), t.data ? function (e) {
        var t = e.$options.data;
        p(t = e._data = "function" == typeof t ? function (e, t) {
          ye();

          try {
            return e.call(t, t);
          } catch (e) {
            return Ge(e, t, "data()"), {};
          } finally {
            ge();
          }
        }(t, e) : t || {}) || (t = {});
        var n = Object.keys(t),
            r = e.$options.props,
            i = (e.$options.methods, n.length);

        for (; i--;) {
          var o = n[i];
          0, r && $(r, o) || K(o) || Cn(e, "_data", o);
        }

        Ee(t, !0);
      }(e) : Ee(e._data = {}, !0), t.computed && function (e, t) {
        var n = e._computedWatchers = Object.create(null),
            r = ce();

        for (var i in t) {
          var o = t[i],
              a = "function" == typeof o ? o : o.get;
          0, r || (n[i] = new xn(e, a || M, M, An)), i in e || On(e, i, o);
        }
      }(e, t.computed), t.watch && t.watch !== oe && function (e, t) {
        for (var n in t) {
          var r = t[n];
          if (Array.isArray(r)) for (var i = 0; i < r.length; i++) {
            En(e, n, r[i]);
          } else En(e, n, r);
        }
      }(e, t.watch);
    }

    var An = {
      lazy: !0
    };

    function On(e, t, n) {
      var r = !ce();
      "function" == typeof n ? ($n.get = r ? Sn(t) : Tn(n), $n.set = M) : ($n.get = n.get ? r && !1 !== n.cache ? Sn(t) : Tn(n.get) : M, $n.set = n.set || M), Object.defineProperty(e, t, $n);
    }

    function Sn(e) {
      return function () {
        var t = this._computedWatchers && this._computedWatchers[e];
        if (t) return t.dirty && t.evaluate(), he.target && t.depend(), t.value;
      };
    }

    function Tn(e) {
      return function () {
        return e.call(this, this);
      };
    }

    function En(e, t, n, r) {
      return p(n) && (r = n, n = n.handler), "string" == typeof n && (n = e[n]), e.$watch(t, n, r);
    }

    var jn = 0;

    function Ln(e) {
      var t = e.options;

      if (e["super"]) {
        var n = Ln(e["super"]);

        if (n !== e.superOptions) {
          e.superOptions = n;

          var r = function (e) {
            var t,
                n = e.options,
                r = e.sealedOptions;

            for (var i in n) {
              n[i] !== r[i] && (t || (t = {}), t[i] = n[i]);
            }

            return t;
          }(e);

          r && L(e.extendOptions, r), (t = e.options = Be(n, e.extendOptions)).name && (t.components[t.name] = e);
        }
      }

      return t;
    }

    function Nn(e) {
      this._init(e);
    }

    function Mn(e) {
      e.cid = 0;
      var t = 1;

      e.extend = function (e) {
        e = e || {};
        var n = this,
            r = n.cid,
            i = e._Ctor || (e._Ctor = {});
        if (i[r]) return i[r];
        var o = e.name || n.options.name;

        var a = function a(e) {
          this._init(e);
        };

        return (a.prototype = Object.create(n.prototype)).constructor = a, a.cid = t++, a.options = Be(n.options, e), a["super"] = n, a.options.props && function (e) {
          var t = e.options.props;

          for (var n in t) {
            Cn(e.prototype, "_props", n);
          }
        }(a), a.options.computed && function (e) {
          var t = e.options.computed;

          for (var n in t) {
            On(e.prototype, n, t[n]);
          }
        }(a), a.extend = n.extend, a.mixin = n.mixin, a.use = n.use, B.forEach(function (e) {
          a[e] = n[e];
        }), o && (a.options.components[o] = a), a.superOptions = n.options, a.extendOptions = e, a.sealedOptions = L({}, a.options), i[r] = a, a;
      };
    }

    function In(e) {
      return e && (e.Ctor.options.name || e.tag);
    }

    function Dn(e, t) {
      return Array.isArray(e) ? e.indexOf(t) > -1 : "string" == typeof e ? e.split(",").indexOf(t) > -1 : !!d(e) && e.test(t);
    }

    function Pn(e, t) {
      var n = e.cache,
          r = e.keys,
          i = e._vnode;

      for (var o in n) {
        var a = n[o];

        if (a) {
          var s = a.name;
          s && !t(s) && Fn(n, o, r, i);
        }
      }
    }

    function Fn(e, t, n, r) {
      var i = e[t];
      !i || r && i.tag === r.tag || i.componentInstance.$destroy(), e[t] = null, w(n, t);
    }

    !function (e) {
      e.prototype._init = function (e) {
        var t = this;
        t._uid = jn++, t._isVue = !0, e && e._isComponent ? function (e, t) {
          var n = e.$options = Object.create(e.constructor.options),
              r = t._parentVnode;
          n.parent = t.parent, n._parentVnode = r;
          var i = r.componentOptions;
          n.propsData = i.propsData, n._parentListeners = i.listeners, n._renderChildren = i.children, n._componentTag = i.tag, t.render && (n.render = t.render, n.staticRenderFns = t.staticRenderFns);
        }(t, e) : t.$options = Be(Ln(t.constructor), e || {}, t), t._renderProxy = t, t._self = t, function (e) {
          var t = e.$options,
              n = t.parent;

          if (n && !t["abstract"]) {
            for (; n.$options["abstract"] && n.$parent;) {
              n = n.$parent;
            }

            n.$children.push(e);
          }

          e.$parent = n, e.$root = n ? n.$root : e, e.$children = [], e.$refs = {}, e._watcher = null, e._inactive = null, e._directInactive = !1, e._isMounted = !1, e._isDestroyed = !1, e._isBeingDestroyed = !1;
        }(t), function (e) {
          e._events = Object.create(null), e._hasHookEvent = !1;
          var t = e.$options._parentListeners;
          t && rn(e, t);
        }(t), function (e) {
          e._vnode = null, e._staticTrees = null;
          var t = e.$options,
              n = e.$vnode = t._parentVnode,
              r = n && n.context;
          e.$slots = bt(t._renderChildren, r), e.$scopedSlots = o, e._c = function (t, n, r, i) {
            return Gt(e, t, n, r, i, !1);
          }, e.$createElement = function (t, n, r, i) {
            return Gt(e, t, n, r, i, !0);
          };
          var i = n && n.data;
          je(e, "$attrs", i && i.attrs || o, null, !0), je(e, "$listeners", t._parentListeners || o, null, !0);
        }(t), ln(t, "beforeCreate"), function (e) {
          var t = _t(e.$options.inject, e);

          t && (Se(!1), Object.keys(t).forEach(function (n) {
            je(e, n, t[n]);
          }), Se(!0));
        }(t), kn(t), function (e) {
          var t = e.$options.provide;
          t && (e._provided = "function" == typeof t ? t.call(e) : t);
        }(t), ln(t, "created"), t.$options.el && t.$mount(t.$options.el);
      };
    }(Nn), function (e) {
      var t = {
        get: function get() {
          return this._data;
        }
      },
          n = {
        get: function get() {
          return this._props;
        }
      };
      Object.defineProperty(e.prototype, "$data", t), Object.defineProperty(e.prototype, "$props", n), e.prototype.$set = Le, e.prototype.$delete = Ne, e.prototype.$watch = function (e, t, n) {
        var r = this;
        if (p(t)) return En(r, e, t, n);
        (n = n || {}).user = !0;
        var i = new xn(r, e, t, n);

        if (n.immediate) {
          var o = 'callback for immediate watcher "' + i.expression + '"';
          ye(), Ze(t, r, [i.value], r, o), ge();
        }

        return function () {
          i.teardown();
        };
      };
    }(Nn), function (e) {
      var t = /^hook:/;
      e.prototype.$on = function (e, n) {
        var r = this;
        if (Array.isArray(e)) for (var i = 0, o = e.length; i < o; i++) {
          r.$on(e[i], n);
        } else (r._events[e] || (r._events[e] = [])).push(n), t.test(e) && (r._hasHookEvent = !0);
        return r;
      }, e.prototype.$once = function (e, t) {
        var n = this;

        function r() {
          n.$off(e, r), t.apply(n, arguments);
        }

        return r.fn = t, n.$on(e, r), n;
      }, e.prototype.$off = function (e, t) {
        var n = this;
        if (!arguments.length) return n._events = Object.create(null), n;

        if (Array.isArray(e)) {
          for (var r = 0, i = e.length; r < i; r++) {
            n.$off(e[r], t);
          }

          return n;
        }

        var o,
            a = n._events[e];
        if (!a) return n;
        if (!t) return n._events[e] = null, n;

        for (var s = a.length; s--;) {
          if ((o = a[s]) === t || o.fn === t) {
            a.splice(s, 1);
            break;
          }
        }

        return n;
      }, e.prototype.$emit = function (e) {
        var t = this,
            n = t._events[e];

        if (n) {
          n = n.length > 1 ? j(n) : n;

          for (var r = j(arguments, 1), i = 'event handler for "' + e + '"', o = 0, a = n.length; o < a; o++) {
            Ze(n[o], t, r, t, i);
          }
        }

        return t;
      };
    }(Nn), function (e) {
      e.prototype._update = function (e, t) {
        var n = this,
            r = n.$el,
            i = n._vnode,
            o = an(n);
        n._vnode = e, n.$el = i ? n.__patch__(i, e) : n.__patch__(n.$el, e, t, !1), o(), r && (r.__vue__ = null), n.$el && (n.$el.__vue__ = n), n.$vnode && n.$parent && n.$vnode === n.$parent._vnode && (n.$parent.$el = n.$el);
      }, e.prototype.$forceUpdate = function () {
        this._watcher && this._watcher.update();
      }, e.prototype.$destroy = function () {
        var e = this;

        if (!e._isBeingDestroyed) {
          ln(e, "beforeDestroy"), e._isBeingDestroyed = !0;
          var t = e.$parent;
          !t || t._isBeingDestroyed || e.$options["abstract"] || w(t.$children, e), e._watcher && e._watcher.teardown();

          for (var n = e._watchers.length; n--;) {
            e._watchers[n].teardown();
          }

          e._data.__ob__ && e._data.__ob__.vmCount--, e._isDestroyed = !0, e.__patch__(e._vnode, null), ln(e, "destroyed"), e.$off(), e.$el && (e.$el.__vue__ = null), e.$vnode && (e.$vnode.parent = null);
        }
      };
    }(Nn), function (e) {
      Ht(e.prototype), e.prototype.$nextTick = function (e) {
        return st(e, this);
      }, e.prototype._render = function () {
        var e,
            t = this,
            n = t.$options,
            r = n.render,
            i = n._parentVnode;
        i && (t.$scopedSlots = $t(i.data.scopedSlots, t.$slots, t.$scopedSlots)), t.$vnode = i;

        try {
          Xt = t, e = r.call(t._renderProxy, t.$createElement);
        } catch (n) {
          Ge(n, t, "render"), e = t._vnode;
        } finally {
          Xt = null;
        }

        return Array.isArray(e) && 1 === e.length && (e = e[0]), e instanceof _e || (e = we()), e.parent = i, e;
      };
    }(Nn);
    var Rn = [String, RegExp, Array],
        Hn = {
      name: "keep-alive",
      "abstract": !0,
      props: {
        include: Rn,
        exclude: Rn,
        max: [String, Number]
      },
      methods: {
        cacheVNode: function cacheVNode() {
          var e = this,
              t = e.cache,
              n = e.keys,
              r = e.vnodeToCache,
              i = e.keyToCache;

          if (r) {
            var o = r.tag,
                a = r.componentInstance,
                s = r.componentOptions;
            t[i] = {
              name: In(s),
              tag: o,
              componentInstance: a
            }, n.push(i), this.max && n.length > parseInt(this.max) && Fn(t, n[0], n, this._vnode), this.vnodeToCache = null;
          }
        }
      },
      created: function created() {
        this.cache = Object.create(null), this.keys = [];
      },
      destroyed: function destroyed() {
        for (var e in this.cache) {
          Fn(this.cache, e, this.keys);
        }
      },
      mounted: function mounted() {
        var e = this;
        this.cacheVNode(), this.$watch("include", function (t) {
          Pn(e, function (e) {
            return Dn(t, e);
          });
        }), this.$watch("exclude", function (t) {
          Pn(e, function (e) {
            return !Dn(t, e);
          });
        });
      },
      updated: function updated() {
        this.cacheVNode();
      },
      render: function render() {
        var e = this.$slots["default"],
            t = Qt(e),
            n = t && t.componentOptions;

        if (n) {
          var r = In(n),
              i = this.include,
              o = this.exclude;
          if (i && (!r || !Dn(i, r)) || o && r && Dn(o, r)) return t;
          var a = this.cache,
              s = this.keys,
              c = null == t.key ? n.Ctor.cid + (n.tag ? "::" + n.tag : "") : t.key;
          a[c] ? (t.componentInstance = a[c].componentInstance, w(s, c), s.push(c)) : (this.vnodeToCache = t, this.keyToCache = c), t.data.keepAlive = !0;
        }

        return t || e && e[0];
      }
    },
        Bn = {
      KeepAlive: Hn
    };
    !function (e) {
      var t = {
        get: function get() {
          return V;
        }
      };
      Object.defineProperty(e, "config", t), e.util = {
        warn: de,
        extend: L,
        mergeOptions: Be,
        defineReactive: je
      }, e.set = Le, e["delete"] = Ne, e.nextTick = st, e.observable = function (e) {
        return Ee(e), e;
      }, e.options = Object.create(null), B.forEach(function (t) {
        e.options[t + "s"] = Object.create(null);
      }), e.options._base = e, L(e.options.components, Bn), function (e) {
        e.use = function (e) {
          var t = this._installedPlugins || (this._installedPlugins = []);
          if (t.indexOf(e) > -1) return this;
          var n = j(arguments, 1);
          return n.unshift(this), "function" == typeof e.install ? e.install.apply(e, n) : "function" == typeof e && e.apply(null, n), t.push(e), this;
        };
      }(e), function (e) {
        e.mixin = function (e) {
          return this.options = Be(this.options, e), this;
        };
      }(e), Mn(e), function (e) {
        B.forEach(function (t) {
          e[t] = function (e, n) {
            return n ? ("component" === t && p(n) && (n.name = n.name || e, n = this.options._base.extend(n)), "directive" === t && "function" == typeof n && (n = {
              bind: n,
              update: n
            }), this.options[t + "s"][e] = n, n) : this.options[t + "s"][e];
          };
        });
      }(e);
    }(Nn), Object.defineProperty(Nn.prototype, "$isServer", {
      get: ce
    }), Object.defineProperty(Nn.prototype, "$ssrContext", {
      get: function get() {
        return this.$vnode && this.$vnode.ssrContext;
      }
    }), Object.defineProperty(Nn, "FunctionalRenderContext", {
      value: Bt
    }), Nn.version = "2.6.14";

    var Un = g("style,class"),
        Vn = g("input,textarea,option,select,progress"),
        zn = function zn(e, t, n) {
      return "value" === n && Vn(e) && "button" !== t || "selected" === n && "option" === e || "checked" === n && "input" === e || "muted" === n && "video" === e;
    },
        Kn = g("contenteditable,draggable,spellcheck"),
        Jn = g("events,caret,typing,plaintext-only"),
        qn = g("allowfullscreen,async,autofocus,autoplay,checked,compact,controls,declare,default,defaultchecked,defaultmuted,defaultselected,defer,disabled,enabled,formnovalidate,hidden,indeterminate,inert,ismap,itemscope,loop,multiple,muted,nohref,noresize,noshade,novalidate,nowrap,open,pauseonexit,readonly,required,reversed,scoped,seamless,selected,sortable,truespeed,typemustmatch,visible"),
        Gn = "http://www.w3.org/1999/xlink",
        Zn = function Zn(e) {
      return ":" === e.charAt(5) && "xlink" === e.slice(0, 5);
    },
        Wn = function Wn(e) {
      return Zn(e) ? e.slice(6, e.length) : "";
    },
        Xn = function Xn(e) {
      return null == e || !1 === e;
    };

    function Yn(e) {
      for (var t = e.data, n = e, r = e; s(r.componentInstance);) {
        (r = r.componentInstance._vnode) && r.data && (t = Qn(r.data, t));
      }

      for (; s(n = n.parent);) {
        n && n.data && (t = Qn(t, n.data));
      }

      return function (e, t) {
        if (s(e) || s(t)) return er(e, tr(t));
        return "";
      }(t.staticClass, t["class"]);
    }

    function Qn(e, t) {
      return {
        staticClass: er(e.staticClass, t.staticClass),
        "class": s(e["class"]) ? [e["class"], t["class"]] : t["class"]
      };
    }

    function er(e, t) {
      return e ? t ? e + " " + t : e : t || "";
    }

    function tr(e) {
      return Array.isArray(e) ? function (e) {
        for (var t, n = "", r = 0, i = e.length; r < i; r++) {
          s(t = tr(e[r])) && "" !== t && (n && (n += " "), n += t);
        }

        return n;
      }(e) : l(e) ? function (e) {
        var t = "";

        for (var n in e) {
          e[n] && (t && (t += " "), t += n);
        }

        return t;
      }(e) : "string" == typeof e ? e : "";
    }

    var nr = {
      svg: "http://www.w3.org/2000/svg",
      math: "http://www.w3.org/1998/Math/MathML"
    },
        rr = g("html,body,base,head,link,meta,style,title,address,article,aside,footer,header,h1,h2,h3,h4,h5,h6,hgroup,nav,section,div,dd,dl,dt,figcaption,figure,picture,hr,img,li,main,ol,p,pre,ul,a,b,abbr,bdi,bdo,br,cite,code,data,dfn,em,i,kbd,mark,q,rp,rt,rtc,ruby,s,samp,small,span,strong,sub,sup,time,u,var,wbr,area,audio,map,track,video,embed,object,param,source,canvas,script,noscript,del,ins,caption,col,colgroup,table,thead,tbody,td,th,tr,button,datalist,fieldset,form,input,label,legend,meter,optgroup,option,output,progress,select,textarea,details,dialog,menu,menuitem,summary,content,element,shadow,template,blockquote,iframe,tfoot"),
        ir = g("svg,animate,circle,clippath,cursor,defs,desc,ellipse,filter,font-face,foreignobject,g,glyph,image,line,marker,mask,missing-glyph,path,pattern,polygon,polyline,rect,switch,symbol,text,textpath,tspan,use,view", !0),
        or = function or(e) {
      return rr(e) || ir(e);
    };

    function ar(e) {
      return ir(e) ? "svg" : "math" === e ? "math" : void 0;
    }

    var sr = Object.create(null);
    var cr = g("text,number,password,search,email,tel,url");

    function ur(e) {
      if ("string" == typeof e) {
        var t = document.querySelector(e);
        return t || document.createElement("div");
      }

      return e;
    }

    var lr = Object.freeze({
      createElement: function createElement(e, t) {
        var n = document.createElement(e);
        return "select" !== e || t.data && t.data.attrs && void 0 !== t.data.attrs.multiple && n.setAttribute("multiple", "multiple"), n;
      },
      createElementNS: function createElementNS(e, t) {
        return document.createElementNS(nr[e], t);
      },
      createTextNode: function createTextNode(e) {
        return document.createTextNode(e);
      },
      createComment: function createComment(e) {
        return document.createComment(e);
      },
      insertBefore: function insertBefore(e, t, n) {
        e.insertBefore(t, n);
      },
      removeChild: function removeChild(e, t) {
        e.removeChild(t);
      },
      appendChild: function appendChild(e, t) {
        e.appendChild(t);
      },
      parentNode: function parentNode(e) {
        return e.parentNode;
      },
      nextSibling: function nextSibling(e) {
        return e.nextSibling;
      },
      tagName: function tagName(e) {
        return e.tagName;
      },
      setTextContent: function setTextContent(e, t) {
        e.textContent = t;
      },
      setStyleScope: function setStyleScope(e, t) {
        e.setAttribute(t, "");
      }
    }),
        fr = {
      create: function create(e, t) {
        pr(t);
      },
      update: function update(e, t) {
        e.data.ref !== t.data.ref && (pr(e, !0), pr(t));
      },
      destroy: function destroy(e) {
        pr(e, !0);
      }
    };

    function pr(e, t) {
      var n = e.data.ref;

      if (s(n)) {
        var r = e.context,
            i = e.componentInstance || e.elm,
            o = r.$refs;
        t ? Array.isArray(o[n]) ? w(o[n], i) : o[n] === i && (o[n] = void 0) : e.data.refInFor ? Array.isArray(o[n]) ? o[n].indexOf(i) < 0 && o[n].push(i) : o[n] = [i] : o[n] = i;
      }
    }

    var dr = new _e("", {}, []),
        vr = ["create", "activate", "update", "remove", "destroy"];

    function hr(e, t) {
      return e.key === t.key && e.asyncFactory === t.asyncFactory && (e.tag === t.tag && e.isComment === t.isComment && s(e.data) === s(t.data) && function (e, t) {
        if ("input" !== e.tag) return !0;
        var n,
            r = s(n = e.data) && s(n = n.attrs) && n.type,
            i = s(n = t.data) && s(n = n.attrs) && n.type;
        return r === i || cr(r) && cr(i);
      }(e, t) || c(e.isAsyncPlaceholder) && a(t.asyncFactory.error));
    }

    function mr(e, t, n) {
      var r,
          i,
          o = {};

      for (r = t; r <= n; ++r) {
        s(i = e[r].key) && (o[i] = r);
      }

      return o;
    }

    var yr = {
      create: gr,
      update: gr,
      destroy: function destroy(e) {
        gr(e, dr);
      }
    };

    function gr(e, t) {
      (e.data.directives || t.data.directives) && function (e, t) {
        var n,
            r,
            i,
            o = e === dr,
            a = t === dr,
            s = br(e.data.directives, e.context),
            c = br(t.data.directives, t.context),
            u = [],
            l = [];

        for (n in c) {
          r = s[n], i = c[n], r ? (i.oldValue = r.value, i.oldArg = r.arg, xr(i, "update", t, e), i.def && i.def.componentUpdated && l.push(i)) : (xr(i, "bind", t, e), i.def && i.def.inserted && u.push(i));
        }

        if (u.length) {
          var f = function f() {
            for (var n = 0; n < u.length; n++) {
              xr(u[n], "inserted", t, e);
            }
          };

          o ? vt(t, "insert", f) : f();
        }

        l.length && vt(t, "postpatch", function () {
          for (var n = 0; n < l.length; n++) {
            xr(l[n], "componentUpdated", t, e);
          }
        });
        if (!o) for (n in s) {
          c[n] || xr(s[n], "unbind", e, e, a);
        }
      }(e, t);
    }

    var _r = Object.create(null);

    function br(e, t) {
      var n,
          r,
          i = Object.create(null);
      if (!e) return i;

      for (n = 0; n < e.length; n++) {
        (r = e[n]).modifiers || (r.modifiers = _r), i[wr(r)] = r, r.def = Ue(t.$options, "directives", r.name);
      }

      return i;
    }

    function wr(e) {
      return e.rawName || e.name + "." + Object.keys(e.modifiers || {}).join(".");
    }

    function xr(e, t, n, r, i) {
      var o = e.def && e.def[t];
      if (o) try {
        o(n.elm, e, n, r, i);
      } catch (r) {
        Ge(r, n.context, "directive " + e.name + " " + t + " hook");
      }
    }

    var $r = [fr, yr];

    function Cr(e, t) {
      var n = t.componentOptions;

      if (!(s(n) && !1 === n.Ctor.options.inheritAttrs || a(e.data.attrs) && a(t.data.attrs))) {
        var r,
            i,
            o = t.elm,
            c = e.data.attrs || {},
            u = t.data.attrs || {};

        for (r in s(u.__ob__) && (u = t.data.attrs = L({}, u)), u) {
          i = u[r], c[r] !== i && kr(o, r, i, t.data.pre);
        }

        for (r in (ee || ne) && u.value !== c.value && kr(o, "value", u.value), c) {
          a(u[r]) && (Zn(r) ? o.removeAttributeNS(Gn, Wn(r)) : Kn(r) || o.removeAttribute(r));
        }
      }
    }

    function kr(e, t, n, r) {
      r || e.tagName.indexOf("-") > -1 ? Ar(e, t, n) : qn(t) ? Xn(n) ? e.removeAttribute(t) : (n = "allowfullscreen" === t && "EMBED" === e.tagName ? "true" : t, e.setAttribute(t, n)) : Kn(t) ? e.setAttribute(t, function (e, t) {
        return Xn(t) || "false" === t ? "false" : "contenteditable" === e && Jn(t) ? t : "true";
      }(t, n)) : Zn(t) ? Xn(n) ? e.removeAttributeNS(Gn, Wn(t)) : e.setAttributeNS(Gn, t, n) : Ar(e, t, n);
    }

    function Ar(e, t, n) {
      if (Xn(n)) e.removeAttribute(t);else {
        if (ee && !te && "TEXTAREA" === e.tagName && "placeholder" === t && "" !== n && !e.__ieph) {
          var r = function r(t) {
            t.stopImmediatePropagation(), e.removeEventListener("input", r);
          };

          e.addEventListener("input", r), e.__ieph = !0;
        }

        e.setAttribute(t, n);
      }
    }

    var Or = {
      create: Cr,
      update: Cr
    };

    function Sr(e, t) {
      var n = t.elm,
          r = t.data,
          i = e.data;

      if (!(a(r.staticClass) && a(r["class"]) && (a(i) || a(i.staticClass) && a(i["class"])))) {
        var o = Yn(t),
            c = n._transitionClasses;
        s(c) && (o = er(o, tr(c))), o !== n._prevClass && (n.setAttribute("class", o), n._prevClass = o);
      }
    }

    var Tr,
        Er,
        jr,
        Lr,
        Nr,
        Mr,
        Ir = {
      create: Sr,
      update: Sr
    },
        Dr = /[\w).+\-_$\]]/;

    function Pr(e) {
      var t,
          n,
          r,
          i,
          o,
          a = !1,
          s = !1,
          c = !1,
          u = !1,
          l = 0,
          f = 0,
          p = 0,
          d = 0;

      for (r = 0; r < e.length; r++) {
        if (n = t, t = e.charCodeAt(r), a) 39 === t && 92 !== n && (a = !1);else if (s) 34 === t && 92 !== n && (s = !1);else if (c) 96 === t && 92 !== n && (c = !1);else if (u) 47 === t && 92 !== n && (u = !1);else if (124 !== t || 124 === e.charCodeAt(r + 1) || 124 === e.charCodeAt(r - 1) || l || f || p) {
          switch (t) {
            case 34:
              s = !0;
              break;

            case 39:
              a = !0;
              break;

            case 96:
              c = !0;
              break;

            case 40:
              p++;
              break;

            case 41:
              p--;
              break;

            case 91:
              f++;
              break;

            case 93:
              f--;
              break;

            case 123:
              l++;
              break;

            case 125:
              l--;
          }

          if (47 === t) {
            for (var v = r - 1, h = void 0; v >= 0 && " " === (h = e.charAt(v)); v--) {
              ;
            }

            h && Dr.test(h) || (u = !0);
          }
        } else void 0 === i ? (d = r + 1, i = e.slice(0, r).trim()) : m();
      }

      function m() {
        (o || (o = [])).push(e.slice(d, r).trim()), d = r + 1;
      }

      if (void 0 === i ? i = e.slice(0, r).trim() : 0 !== d && m(), o) for (r = 0; r < o.length; r++) {
        i = Fr(i, o[r]);
      }
      return i;
    }

    function Fr(e, t) {
      var n = t.indexOf("(");
      if (n < 0) return '_f("' + t + '")(' + e + ")";
      var r = t.slice(0, n),
          i = t.slice(n + 1);
      return '_f("' + r + '")(' + e + (")" !== i ? "," + i : i);
    }

    function Rr(e, t) {
      console.error("[Vue compiler]: " + e);
    }

    function Hr(e, t) {
      return e ? e.map(function (e) {
        return e[t];
      }).filter(function (e) {
        return e;
      }) : [];
    }

    function Br(e, t, n, r, i) {
      (e.props || (e.props = [])).push(Wr({
        name: t,
        value: n,
        dynamic: i
      }, r)), e.plain = !1;
    }

    function Ur(e, t, n, r, i) {
      (i ? e.dynamicAttrs || (e.dynamicAttrs = []) : e.attrs || (e.attrs = [])).push(Wr({
        name: t,
        value: n,
        dynamic: i
      }, r)), e.plain = !1;
    }

    function Vr(e, t, n, r) {
      e.attrsMap[t] = n, e.attrsList.push(Wr({
        name: t,
        value: n
      }, r));
    }

    function zr(e, t, n, r, i, o, a, s) {
      (e.directives || (e.directives = [])).push(Wr({
        name: t,
        rawName: n,
        value: r,
        arg: i,
        isDynamicArg: o,
        modifiers: a
      }, s)), e.plain = !1;
    }

    function Kr(e, t, n) {
      return n ? "_p(" + t + ',"' + e + '")' : e + t;
    }

    function Jr(e, t, n, r, i, a, s, c) {
      var u;
      (r = r || o).right ? c ? t = "(" + t + ")==='click'?'contextmenu':(" + t + ")" : "click" === t && (t = "contextmenu", delete r.right) : r.middle && (c ? t = "(" + t + ")==='click'?'mouseup':(" + t + ")" : "click" === t && (t = "mouseup")), r.capture && (delete r.capture, t = Kr("!", t, c)), r.once && (delete r.once, t = Kr("~", t, c)), r.passive && (delete r.passive, t = Kr("&", t, c)), r["native"] ? (delete r["native"], u = e.nativeEvents || (e.nativeEvents = {})) : u = e.events || (e.events = {});
      var l = Wr({
        value: n.trim(),
        dynamic: c
      }, s);
      r !== o && (l.modifiers = r);
      var f = u[t];
      Array.isArray(f) ? i ? f.unshift(l) : f.push(l) : u[t] = f ? i ? [l, f] : [f, l] : l, e.plain = !1;
    }

    function qr(e, t, n) {
      var r = Gr(e, ":" + t) || Gr(e, "v-bind:" + t);
      if (null != r) return Pr(r);

      if (!1 !== n) {
        var i = Gr(e, t);
        if (null != i) return JSON.stringify(i);
      }
    }

    function Gr(e, t, n) {
      var r;
      if (null != (r = e.attrsMap[t])) for (var i = e.attrsList, o = 0, a = i.length; o < a; o++) {
        if (i[o].name === t) {
          i.splice(o, 1);
          break;
        }
      }
      return n && delete e.attrsMap[t], r;
    }

    function Zr(e, t) {
      for (var n = e.attrsList, r = 0, i = n.length; r < i; r++) {
        var o = n[r];
        if (t.test(o.name)) return n.splice(r, 1), o;
      }
    }

    function Wr(e, t) {
      return t && (null != t.start && (e.start = t.start), null != t.end && (e.end = t.end)), e;
    }

    function Xr(e, t, n) {
      var r = n || {},
          i = r.number,
          o = "$$v",
          a = o;
      r.trim && (a = "(typeof $$v === 'string'? $$v.trim(): $$v)"), i && (a = "_n(" + a + ")");
      var s = Yr(t, a);
      e.model = {
        value: "(" + t + ")",
        expression: JSON.stringify(t),
        callback: "function ($$v) {" + s + "}"
      };
    }

    function Yr(e, t) {
      var n = function (e) {
        if (e = e.trim(), Tr = e.length, e.indexOf("[") < 0 || e.lastIndexOf("]") < Tr - 1) return (Lr = e.lastIndexOf(".")) > -1 ? {
          exp: e.slice(0, Lr),
          key: '"' + e.slice(Lr + 1) + '"'
        } : {
          exp: e,
          key: null
        };
        Er = e, Lr = Nr = Mr = 0;

        for (; !ei();) {
          ti(jr = Qr()) ? ri(jr) : 91 === jr && ni(jr);
        }

        return {
          exp: e.slice(0, Nr),
          key: e.slice(Nr + 1, Mr)
        };
      }(e);

      return null === n.key ? e + "=" + t : "$set(" + n.exp + ", " + n.key + ", " + t + ")";
    }

    function Qr() {
      return Er.charCodeAt(++Lr);
    }

    function ei() {
      return Lr >= Tr;
    }

    function ti(e) {
      return 34 === e || 39 === e;
    }

    function ni(e) {
      var t = 1;

      for (Nr = Lr; !ei();) {
        if (ti(e = Qr())) ri(e);else if (91 === e && t++, 93 === e && t--, 0 === t) {
          Mr = Lr;
          break;
        }
      }
    }

    function ri(e) {
      for (var t = e; !ei() && (e = Qr()) !== t;) {
        ;
      }
    }

    var ii,
        oi = "__r";

    function ai(e, t, n) {
      var r = ii;
      return function i() {
        var o = t.apply(null, arguments);
        null !== o && ui(e, i, n, r);
      };
    }

    var si = Qe && !(ie && Number(ie[1]) <= 53);

    function ci(e, t, n, r) {
      if (si) {
        var i = yn,
            o = t;

        t = o._wrapper = function (e) {
          if (e.target === e.currentTarget || e.timeStamp >= i || e.timeStamp <= 0 || e.target.ownerDocument !== document) return o.apply(this, arguments);
        };
      }

      ii.addEventListener(e, t, ae ? {
        capture: n,
        passive: r
      } : n);
    }

    function ui(e, t, n, r) {
      (r || ii).removeEventListener(e, t._wrapper || t, n);
    }

    function li(e, t) {
      if (!a(e.data.on) || !a(t.data.on)) {
        var n = t.data.on || {},
            r = e.data.on || {};
        ii = t.elm, function (e) {
          if (s(e.__r)) {
            var t = ee ? "change" : "input";
            e[t] = [].concat(e.__r, e[t] || []), delete e.__r;
          }

          s(e.__c) && (e.change = [].concat(e.__c, e.change || []), delete e.__c);
        }(n), dt(n, r, ci, ui, ai, t.context), ii = void 0;
      }
    }

    var fi,
        pi = {
      create: li,
      update: li
    };

    function di(e, t) {
      if (!a(e.data.domProps) || !a(t.data.domProps)) {
        var n,
            r,
            i = t.elm,
            o = e.data.domProps || {},
            c = t.data.domProps || {};

        for (n in s(c.__ob__) && (c = t.data.domProps = L({}, c)), o) {
          n in c || (i[n] = "");
        }

        for (n in c) {
          if (r = c[n], "textContent" === n || "innerHTML" === n) {
            if (t.children && (t.children.length = 0), r === o[n]) continue;
            1 === i.childNodes.length && i.removeChild(i.childNodes[0]);
          }

          if ("value" === n && "PROGRESS" !== i.tagName) {
            i._value = r;
            var u = a(r) ? "" : String(r);
            vi(i, u) && (i.value = u);
          } else if ("innerHTML" === n && ir(i.tagName) && a(i.innerHTML)) {
            (fi = fi || document.createElement("div")).innerHTML = "<svg>" + r + "</svg>";

            for (var l = fi.firstChild; i.firstChild;) {
              i.removeChild(i.firstChild);
            }

            for (; l.firstChild;) {
              i.appendChild(l.firstChild);
            }
          } else if (r !== o[n]) try {
            i[n] = r;
          } catch (e) {}
        }
      }
    }

    function vi(e, t) {
      return !e.composing && ("OPTION" === e.tagName || function (e, t) {
        var n = !0;

        try {
          n = document.activeElement !== e;
        } catch (e) {}

        return n && e.value !== t;
      }(e, t) || function (e, t) {
        var n = e.value,
            r = e._vModifiers;

        if (s(r)) {
          if (r.number) return y(n) !== y(t);
          if (r.trim) return n.trim() !== t.trim();
        }

        return n !== t;
      }(e, t));
    }

    var hi = {
      create: di,
      update: di
    },
        mi = C(function (e) {
      var t = {},
          n = /:(.+)/;
      return e.split(/;(?![^(]*\))/g).forEach(function (e) {
        if (e) {
          var r = e.split(n);
          r.length > 1 && (t[r[0].trim()] = r[1].trim());
        }
      }), t;
    });

    function yi(e) {
      var t = gi(e.style);
      return e.staticStyle ? L(e.staticStyle, t) : t;
    }

    function gi(e) {
      return Array.isArray(e) ? N(e) : "string" == typeof e ? mi(e) : e;
    }

    var _i,
        bi = /^--/,
        wi = /\s*!important$/,
        xi = function xi(e, t, n) {
      if (bi.test(t)) e.style.setProperty(t, n);else if (wi.test(n)) e.style.setProperty(T(t), n.replace(wi, ""), "important");else {
        var r = Ci(t);
        if (Array.isArray(n)) for (var i = 0, o = n.length; i < o; i++) {
          e.style[r] = n[i];
        } else e.style[r] = n;
      }
    },
        $i = ["Webkit", "Moz", "ms"],
        Ci = C(function (e) {
      if (_i = _i || document.createElement("div").style, "filter" !== (e = A(e)) && e in _i) return e;

      for (var t = e.charAt(0).toUpperCase() + e.slice(1), n = 0; n < $i.length; n++) {
        var r = $i[n] + t;
        if (r in _i) return r;
      }
    });

    function ki(e, t) {
      var n = t.data,
          r = e.data;

      if (!(a(n.staticStyle) && a(n.style) && a(r.staticStyle) && a(r.style))) {
        var i,
            o,
            c = t.elm,
            u = r.staticStyle,
            l = r.normalizedStyle || r.style || {},
            f = u || l,
            p = gi(t.data.style) || {};
        t.data.normalizedStyle = s(p.__ob__) ? L({}, p) : p;

        var d = function (e, t) {
          var n,
              r = {};
          if (t) for (var i = e; i.componentInstance;) {
            (i = i.componentInstance._vnode) && i.data && (n = yi(i.data)) && L(r, n);
          }
          (n = yi(e.data)) && L(r, n);

          for (var o = e; o = o.parent;) {
            o.data && (n = yi(o.data)) && L(r, n);
          }

          return r;
        }(t, !0);

        for (o in f) {
          a(d[o]) && xi(c, o, "");
        }

        for (o in d) {
          (i = d[o]) !== f[o] && xi(c, o, null == i ? "" : i);
        }
      }
    }

    var Ai = {
      create: ki,
      update: ki
    },
        Oi = /\s+/;

    function Si(e, t) {
      if (t && (t = t.trim())) if (e.classList) t.indexOf(" ") > -1 ? t.split(Oi).forEach(function (t) {
        return e.classList.add(t);
      }) : e.classList.add(t);else {
        var n = " " + (e.getAttribute("class") || "") + " ";
        n.indexOf(" " + t + " ") < 0 && e.setAttribute("class", (n + t).trim());
      }
    }

    function Ti(e, t) {
      if (t && (t = t.trim())) if (e.classList) t.indexOf(" ") > -1 ? t.split(Oi).forEach(function (t) {
        return e.classList.remove(t);
      }) : e.classList.remove(t), e.classList.length || e.removeAttribute("class");else {
        for (var n = " " + (e.getAttribute("class") || "") + " ", r = " " + t + " "; n.indexOf(r) >= 0;) {
          n = n.replace(r, " ");
        }

        (n = n.trim()) ? e.setAttribute("class", n) : e.removeAttribute("class");
      }
    }

    function Ei(e) {
      if (e) {
        if ("object" == _typeof(e)) {
          var t = {};
          return !1 !== e.css && L(t, ji(e.name || "v")), L(t, e), t;
        }

        return "string" == typeof e ? ji(e) : void 0;
      }
    }

    var ji = C(function (e) {
      return {
        enterClass: e + "-enter",
        enterToClass: e + "-enter-to",
        enterActiveClass: e + "-enter-active",
        leaveClass: e + "-leave",
        leaveToClass: e + "-leave-to",
        leaveActiveClass: e + "-leave-active"
      };
    }),
        Li = W && !te,
        Ni = "transition",
        Mi = "animation",
        Ii = "transition",
        Di = "transitionend",
        Pi = "animation",
        Fi = "animationend";
    Li && (void 0 === window.ontransitionend && void 0 !== window.onwebkittransitionend && (Ii = "WebkitTransition", Di = "webkitTransitionEnd"), void 0 === window.onanimationend && void 0 !== window.onwebkitanimationend && (Pi = "WebkitAnimation", Fi = "webkitAnimationEnd"));
    var Ri = W ? window.requestAnimationFrame ? window.requestAnimationFrame.bind(window) : setTimeout : function (e) {
      return e();
    };

    function Hi(e) {
      Ri(function () {
        Ri(e);
      });
    }

    function Bi(e, t) {
      var n = e._transitionClasses || (e._transitionClasses = []);
      n.indexOf(t) < 0 && (n.push(t), Si(e, t));
    }

    function Ui(e, t) {
      e._transitionClasses && w(e._transitionClasses, t), Ti(e, t);
    }

    function Vi(e, t, n) {
      var r = Ki(e, t),
          i = r.type,
          o = r.timeout,
          a = r.propCount;
      if (!i) return n();

      var s = i === Ni ? Di : Fi,
          c = 0,
          u = function u() {
        e.removeEventListener(s, l), n();
      },
          l = function l(t) {
        t.target === e && ++c >= a && u();
      };

      setTimeout(function () {
        c < a && u();
      }, o + 1), e.addEventListener(s, l);
    }

    var zi = /\b(transform|all)(,|$)/;

    function Ki(e, t) {
      var n,
          r = window.getComputedStyle(e),
          i = (r[Ii + "Delay"] || "").split(", "),
          o = (r[Ii + "Duration"] || "").split(", "),
          a = Ji(i, o),
          s = (r[Pi + "Delay"] || "").split(", "),
          c = (r[Pi + "Duration"] || "").split(", "),
          u = Ji(s, c),
          l = 0,
          f = 0;
      return t === Ni ? a > 0 && (n = Ni, l = a, f = o.length) : t === Mi ? u > 0 && (n = Mi, l = u, f = c.length) : f = (n = (l = Math.max(a, u)) > 0 ? a > u ? Ni : Mi : null) ? n === Ni ? o.length : c.length : 0, {
        type: n,
        timeout: l,
        propCount: f,
        hasTransform: n === Ni && zi.test(r[Ii + "Property"])
      };
    }

    function Ji(e, t) {
      for (; e.length < t.length;) {
        e = e.concat(e);
      }

      return Math.max.apply(null, t.map(function (t, n) {
        return qi(t) + qi(e[n]);
      }));
    }

    function qi(e) {
      return 1e3 * Number(e.slice(0, -1).replace(",", "."));
    }

    function Gi(e, t) {
      var n = e.elm;
      s(n._leaveCb) && (n._leaveCb.cancelled = !0, n._leaveCb());
      var r = Ei(e.data.transition);

      if (!a(r) && !s(n._enterCb) && 1 === n.nodeType) {
        for (var i = r.css, o = r.type, c = r.enterClass, u = r.enterToClass, f = r.enterActiveClass, p = r.appearClass, d = r.appearToClass, v = r.appearActiveClass, h = r.beforeEnter, m = r.enter, g = r.afterEnter, _ = r.enterCancelled, b = r.beforeAppear, w = r.appear, x = r.afterAppear, $ = r.appearCancelled, C = r.duration, k = on, A = on.$vnode; A && A.parent;) {
          k = A.context, A = A.parent;
        }

        var O = !k._isMounted || !e.isRootInsert;

        if (!O || w || "" === w) {
          var S = O && p ? p : c,
              T = O && v ? v : f,
              E = O && d ? d : u,
              j = O && b || h,
              L = O && "function" == typeof w ? w : m,
              N = O && x || g,
              M = O && $ || _,
              I = y(l(C) ? C.enter : C);
          0;
          var D = !1 !== i && !te,
              P = Xi(L),
              F = n._enterCb = R(function () {
            D && (Ui(n, E), Ui(n, T)), F.cancelled ? (D && Ui(n, S), M && M(n)) : N && N(n), n._enterCb = null;
          });
          e.data.show || vt(e, "insert", function () {
            var t = n.parentNode,
                r = t && t._pending && t._pending[e.key];
            r && r.tag === e.tag && r.elm._leaveCb && r.elm._leaveCb(), L && L(n, F);
          }), j && j(n), D && (Bi(n, S), Bi(n, T), Hi(function () {
            Ui(n, S), F.cancelled || (Bi(n, E), P || (Wi(I) ? setTimeout(F, I) : Vi(n, o, F)));
          })), e.data.show && (t && t(), L && L(n, F)), D || P || F();
        }
      }
    }

    function Zi(e, t) {
      var n = e.elm;
      s(n._enterCb) && (n._enterCb.cancelled = !0, n._enterCb());
      var r = Ei(e.data.transition);
      if (a(r) || 1 !== n.nodeType) return t();

      if (!s(n._leaveCb)) {
        var i = r.css,
            o = r.type,
            c = r.leaveClass,
            u = r.leaveToClass,
            f = r.leaveActiveClass,
            p = r.beforeLeave,
            d = r.leave,
            v = r.afterLeave,
            h = r.leaveCancelled,
            m = r.delayLeave,
            g = r.duration,
            _ = !1 !== i && !te,
            b = Xi(d),
            w = y(l(g) ? g.leave : g);

        0;
        var x = n._leaveCb = R(function () {
          n.parentNode && n.parentNode._pending && (n.parentNode._pending[e.key] = null), _ && (Ui(n, u), Ui(n, f)), x.cancelled ? (_ && Ui(n, c), h && h(n)) : (t(), v && v(n)), n._leaveCb = null;
        });
        m ? m($) : $();
      }

      function $() {
        x.cancelled || (!e.data.show && n.parentNode && ((n.parentNode._pending || (n.parentNode._pending = {}))[e.key] = e), p && p(n), _ && (Bi(n, c), Bi(n, f), Hi(function () {
          Ui(n, c), x.cancelled || (Bi(n, u), b || (Wi(w) ? setTimeout(x, w) : Vi(n, o, x)));
        })), d && d(n, x), _ || b || x());
      }
    }

    function Wi(e) {
      return "number" == typeof e && !isNaN(e);
    }

    function Xi(e) {
      if (a(e)) return !1;
      var t = e.fns;
      return s(t) ? Xi(Array.isArray(t) ? t[0] : t) : (e._length || e.length) > 1;
    }

    function Yi(e, t) {
      !0 !== t.data.show && Gi(t);
    }

    var Qi = function (e) {
      var t,
          n,
          r = {},
          i = e.modules,
          o = e.nodeOps;

      for (t = 0; t < vr.length; ++t) {
        for (r[vr[t]] = [], n = 0; n < i.length; ++n) {
          s(i[n][vr[t]]) && r[vr[t]].push(i[n][vr[t]]);
        }
      }

      function l(e) {
        var t = o.parentNode(e);
        s(t) && o.removeChild(t, e);
      }

      function f(e, t, n, i, a, u, l) {
        if (s(e.elm) && s(u) && (e = u[l] = $e(e)), e.isRootInsert = !a, !function (e, t, n, i) {
          var o = e.data;

          if (s(o)) {
            var a = s(e.componentInstance) && o.keepAlive;
            if (s(o = o.hook) && s(o = o.init) && o(e, !1), s(e.componentInstance)) return p(e, t), d(n, e.elm, i), c(a) && function (e, t, n, i) {
              var o,
                  a = e;

              for (; a.componentInstance;) {
                if (s(o = (a = a.componentInstance._vnode).data) && s(o = o.transition)) {
                  for (o = 0; o < r.activate.length; ++o) {
                    r.activate[o](dr, a);
                  }

                  t.push(a);
                  break;
                }
              }

              d(n, e.elm, i);
            }(e, t, n, i), !0;
          }
        }(e, t, n, i)) {
          var f = e.data,
              h = e.children,
              g = e.tag;
          s(g) ? (e.elm = e.ns ? o.createElementNS(e.ns, g) : o.createElement(g, e), y(e), v(e, h, t), s(f) && m(e, t), d(n, e.elm, i)) : c(e.isComment) ? (e.elm = o.createComment(e.text), d(n, e.elm, i)) : (e.elm = o.createTextNode(e.text), d(n, e.elm, i));
        }
      }

      function p(e, t) {
        s(e.data.pendingInsert) && (t.push.apply(t, e.data.pendingInsert), e.data.pendingInsert = null), e.elm = e.componentInstance.$el, h(e) ? (m(e, t), y(e)) : (pr(e), t.push(e));
      }

      function d(e, t, n) {
        s(e) && (s(n) ? o.parentNode(n) === e && o.insertBefore(e, t, n) : o.appendChild(e, t));
      }

      function v(e, t, n) {
        if (Array.isArray(t)) {
          0;

          for (var r = 0; r < t.length; ++r) {
            f(t[r], n, e.elm, null, !0, t, r);
          }
        } else u(e.text) && o.appendChild(e.elm, o.createTextNode(String(e.text)));
      }

      function h(e) {
        for (; e.componentInstance;) {
          e = e.componentInstance._vnode;
        }

        return s(e.tag);
      }

      function m(e, n) {
        for (var i = 0; i < r.create.length; ++i) {
          r.create[i](dr, e);
        }

        s(t = e.data.hook) && (s(t.create) && t.create(dr, e), s(t.insert) && n.push(e));
      }

      function y(e) {
        var t;
        if (s(t = e.fnScopeId)) o.setStyleScope(e.elm, t);else for (var n = e; n;) {
          s(t = n.context) && s(t = t.$options._scopeId) && o.setStyleScope(e.elm, t), n = n.parent;
        }
        s(t = on) && t !== e.context && t !== e.fnContext && s(t = t.$options._scopeId) && o.setStyleScope(e.elm, t);
      }

      function _(e, t, n, r, i, o) {
        for (; r <= i; ++r) {
          f(n[r], o, e, t, !1, n, r);
        }
      }

      function b(e) {
        var t,
            n,
            i = e.data;
        if (s(i)) for (s(t = i.hook) && s(t = t.destroy) && t(e), t = 0; t < r.destroy.length; ++t) {
          r.destroy[t](e);
        }
        if (s(t = e.children)) for (n = 0; n < e.children.length; ++n) {
          b(e.children[n]);
        }
      }

      function w(e, t, n) {
        for (; t <= n; ++t) {
          var r = e[t];
          s(r) && (s(r.tag) ? (x(r), b(r)) : l(r.elm));
        }
      }

      function x(e, t) {
        if (s(t) || s(e.data)) {
          var n,
              i = r.remove.length + 1;

          for (s(t) ? t.listeners += i : t = function (e, t) {
            function n() {
              0 == --n.listeners && l(e);
            }

            return n.listeners = t, n;
          }(e.elm, i), s(n = e.componentInstance) && s(n = n._vnode) && s(n.data) && x(n, t), n = 0; n < r.remove.length; ++n) {
            r.remove[n](e, t);
          }

          s(n = e.data.hook) && s(n = n.remove) ? n(e, t) : t();
        } else l(e.elm);
      }

      function $(e, t, n, r) {
        for (var i = n; i < r; i++) {
          var o = t[i];
          if (s(o) && hr(e, o)) return i;
        }
      }

      function C(e, t, n, i, u, l) {
        if (e !== t) {
          s(t.elm) && s(i) && (t = i[u] = $e(t));
          var p = t.elm = e.elm;
          if (c(e.isAsyncPlaceholder)) s(t.asyncFactory.resolved) ? O(e.elm, t, n) : t.isAsyncPlaceholder = !0;else if (c(t.isStatic) && c(e.isStatic) && t.key === e.key && (c(t.isCloned) || c(t.isOnce))) t.componentInstance = e.componentInstance;else {
            var d,
                v = t.data;
            s(v) && s(d = v.hook) && s(d = d.prepatch) && d(e, t);
            var m = e.children,
                y = t.children;

            if (s(v) && h(t)) {
              for (d = 0; d < r.update.length; ++d) {
                r.update[d](e, t);
              }

              s(d = v.hook) && s(d = d.update) && d(e, t);
            }

            a(t.text) ? s(m) && s(y) ? m !== y && function (e, t, n, r, i) {
              var c,
                  u,
                  l,
                  p = 0,
                  d = 0,
                  v = t.length - 1,
                  h = t[0],
                  m = t[v],
                  y = n.length - 1,
                  g = n[0],
                  b = n[y],
                  x = !i;

              for (; p <= v && d <= y;) {
                a(h) ? h = t[++p] : a(m) ? m = t[--v] : hr(h, g) ? (C(h, g, r, n, d), h = t[++p], g = n[++d]) : hr(m, b) ? (C(m, b, r, n, y), m = t[--v], b = n[--y]) : hr(h, b) ? (C(h, b, r, n, y), x && o.insertBefore(e, h.elm, o.nextSibling(m.elm)), h = t[++p], b = n[--y]) : hr(m, g) ? (C(m, g, r, n, d), x && o.insertBefore(e, m.elm, h.elm), m = t[--v], g = n[++d]) : (a(c) && (c = mr(t, p, v)), a(u = s(g.key) ? c[g.key] : $(g, t, p, v)) ? f(g, r, e, h.elm, !1, n, d) : hr(l = t[u], g) ? (C(l, g, r, n, d), t[u] = void 0, x && o.insertBefore(e, l.elm, h.elm)) : f(g, r, e, h.elm, !1, n, d), g = n[++d]);
              }

              p > v ? _(e, a(n[y + 1]) ? null : n[y + 1].elm, n, d, y, r) : d > y && w(t, p, v);
            }(p, m, y, n, l) : s(y) ? (s(e.text) && o.setTextContent(p, ""), _(p, null, y, 0, y.length - 1, n)) : s(m) ? w(m, 0, m.length - 1) : s(e.text) && o.setTextContent(p, "") : e.text !== t.text && o.setTextContent(p, t.text), s(v) && s(d = v.hook) && s(d = d.postpatch) && d(e, t);
          }
        }
      }

      function k(e, t, n) {
        if (c(n) && s(e.parent)) e.parent.data.pendingInsert = t;else for (var r = 0; r < t.length; ++r) {
          t[r].data.hook.insert(t[r]);
        }
      }

      var A = g("attrs,class,staticClass,staticStyle,key");

      function O(e, t, n, r) {
        var i,
            o = t.tag,
            a = t.data,
            u = t.children;
        if (r = r || a && a.pre, t.elm = e, c(t.isComment) && s(t.asyncFactory)) return t.isAsyncPlaceholder = !0, !0;
        if (s(a) && (s(i = a.hook) && s(i = i.init) && i(t, !0), s(i = t.componentInstance))) return p(t, n), !0;

        if (s(o)) {
          if (s(u)) if (e.hasChildNodes()) {
            if (s(i = a) && s(i = i.domProps) && s(i = i.innerHTML)) {
              if (i !== e.innerHTML) return !1;
            } else {
              for (var l = !0, f = e.firstChild, d = 0; d < u.length; d++) {
                if (!f || !O(f, u[d], n, r)) {
                  l = !1;
                  break;
                }

                f = f.nextSibling;
              }

              if (!l || f) return !1;
            }
          } else v(t, u, n);

          if (s(a)) {
            var h = !1;

            for (var y in a) {
              if (!A(y)) {
                h = !0, m(t, n);
                break;
              }
            }

            !h && a["class"] && ut(a["class"]);
          }
        } else e.data !== t.text && (e.data = t.text);

        return !0;
      }

      return function (e, t, n, i) {
        if (!a(t)) {
          var u,
              l = !1,
              p = [];
          if (a(e)) l = !0, f(t, p);else {
            var d = s(e.nodeType);
            if (!d && hr(e, t)) C(e, t, p, null, null, i);else {
              if (d) {
                if (1 === e.nodeType && e.hasAttribute(H) && (e.removeAttribute(H), n = !0), c(n) && O(e, t, p)) return k(t, p, !0), e;
                u = e, e = new _e(o.tagName(u).toLowerCase(), {}, [], void 0, u);
              }

              var v = e.elm,
                  m = o.parentNode(v);
              if (f(t, p, v._leaveCb ? null : m, o.nextSibling(v)), s(t.parent)) for (var y = t.parent, g = h(t); y;) {
                for (var _ = 0; _ < r.destroy.length; ++_) {
                  r.destroy[_](y);
                }

                if (y.elm = t.elm, g) {
                  for (var x = 0; x < r.create.length; ++x) {
                    r.create[x](dr, y);
                  }

                  var $ = y.data.hook.insert;
                  if ($.merged) for (var A = 1; A < $.fns.length; A++) {
                    $.fns[A]();
                  }
                } else pr(y);

                y = y.parent;
              }
              s(m) ? w([e], 0, 0) : s(e.tag) && b(e);
            }
          }
          return k(t, p, l), t.elm;
        }

        s(e) && b(e);
      };
    }({
      nodeOps: lr,
      modules: [Or, Ir, pi, hi, Ai, W ? {
        create: Yi,
        activate: Yi,
        remove: function remove(e, t) {
          !0 !== e.data.show ? Zi(e, t) : t();
        }
      } : {}].concat($r)
    });

    te && document.addEventListener("selectionchange", function () {
      var e = document.activeElement;
      e && e.vmodel && so(e, "input");
    });
    var eo = {
      inserted: function inserted(e, t, n, r) {
        "select" === n.tag ? (r.elm && !r.elm._vOptions ? vt(n, "postpatch", function () {
          eo.componentUpdated(e, t, n);
        }) : to(e, t, n.context), e._vOptions = [].map.call(e.options, io)) : ("textarea" === n.tag || cr(e.type)) && (e._vModifiers = t.modifiers, t.modifiers.lazy || (e.addEventListener("compositionstart", oo), e.addEventListener("compositionend", ao), e.addEventListener("change", ao), te && (e.vmodel = !0)));
      },
      componentUpdated: function componentUpdated(e, t, n) {
        if ("select" === n.tag) {
          to(e, t, n.context);
          var r = e._vOptions,
              i = e._vOptions = [].map.call(e.options, io);
          if (i.some(function (e, t) {
            return !P(e, r[t]);
          })) (e.multiple ? t.value.some(function (e) {
            return ro(e, i);
          }) : t.value !== t.oldValue && ro(t.value, i)) && so(e, "change");
        }
      }
    };

    function to(e, t, n) {
      no(e, t, n), (ee || ne) && setTimeout(function () {
        no(e, t, n);
      }, 0);
    }

    function no(e, t, n) {
      var r = t.value,
          i = e.multiple;

      if (!i || Array.isArray(r)) {
        for (var o, a, s = 0, c = e.options.length; s < c; s++) {
          if (a = e.options[s], i) o = F(r, io(a)) > -1, a.selected !== o && (a.selected = o);else if (P(io(a), r)) return void (e.selectedIndex !== s && (e.selectedIndex = s));
        }

        i || (e.selectedIndex = -1);
      }
    }

    function ro(e, t) {
      return t.every(function (t) {
        return !P(t, e);
      });
    }

    function io(e) {
      return "_value" in e ? e._value : e.value;
    }

    function oo(e) {
      e.target.composing = !0;
    }

    function ao(e) {
      e.target.composing && (e.target.composing = !1, so(e.target, "input"));
    }

    function so(e, t) {
      var n = document.createEvent("HTMLEvents");
      n.initEvent(t, !0, !0), e.dispatchEvent(n);
    }

    function co(e) {
      return !e.componentInstance || e.data && e.data.transition ? e : co(e.componentInstance._vnode);
    }

    var uo = {
      bind: function bind(e, t, n) {
        var r = t.value,
            i = (n = co(n)).data && n.data.transition,
            o = e.__vOriginalDisplay = "none" === e.style.display ? "" : e.style.display;
        r && i ? (n.data.show = !0, Gi(n, function () {
          e.style.display = o;
        })) : e.style.display = r ? o : "none";
      },
      update: function update(e, t, n) {
        var r = t.value;
        !r != !t.oldValue && ((n = co(n)).data && n.data.transition ? (n.data.show = !0, r ? Gi(n, function () {
          e.style.display = e.__vOriginalDisplay;
        }) : Zi(n, function () {
          e.style.display = "none";
        })) : e.style.display = r ? e.__vOriginalDisplay : "none");
      },
      unbind: function unbind(e, t, n, r, i) {
        i || (e.style.display = e.__vOriginalDisplay);
      }
    },
        lo = {
      model: eo,
      show: uo
    },
        fo = {
      name: String,
      appear: Boolean,
      css: Boolean,
      mode: String,
      type: String,
      enterClass: String,
      leaveClass: String,
      enterToClass: String,
      leaveToClass: String,
      enterActiveClass: String,
      leaveActiveClass: String,
      appearClass: String,
      appearActiveClass: String,
      appearToClass: String,
      duration: [Number, String, Object]
    };

    function po(e) {
      var t = e && e.componentOptions;
      return t && t.Ctor.options["abstract"] ? po(Qt(t.children)) : e;
    }

    function vo(e) {
      var t = {},
          n = e.$options;

      for (var r in n.propsData) {
        t[r] = e[r];
      }

      var i = n._parentListeners;

      for (var o in i) {
        t[A(o)] = i[o];
      }

      return t;
    }

    function ho(e, t) {
      if (/\d-keep-alive$/.test(t.tag)) return e("keep-alive", {
        props: t.componentOptions.propsData
      });
    }

    var mo = function mo(e) {
      return e.tag || xt(e);
    },
        yo = function yo(e) {
      return "show" === e.name;
    },
        go = {
      name: "transition",
      props: fo,
      "abstract": !0,
      render: function render(e) {
        var t = this,
            n = this.$slots["default"];

        if (n && (n = n.filter(mo)).length) {
          0;
          var r = this.mode;
          0;
          var i = n[0];
          if (function (e) {
            for (; e = e.parent;) {
              if (e.data.transition) return !0;
            }
          }(this.$vnode)) return i;
          var o = po(i);
          if (!o) return i;
          if (this._leaving) return ho(e, i);
          var a = "__transition-" + this._uid + "-";
          o.key = null == o.key ? o.isComment ? a + "comment" : a + o.tag : u(o.key) ? 0 === String(o.key).indexOf(a) ? o.key : a + o.key : o.key;
          var s = (o.data || (o.data = {})).transition = vo(this),
              c = this._vnode,
              l = po(c);

          if (o.data.directives && o.data.directives.some(yo) && (o.data.show = !0), l && l.data && !function (e, t) {
            return t.key === e.key && t.tag === e.tag;
          }(o, l) && !xt(l) && (!l.componentInstance || !l.componentInstance._vnode.isComment)) {
            var f = l.data.transition = L({}, s);
            if ("out-in" === r) return this._leaving = !0, vt(f, "afterLeave", function () {
              t._leaving = !1, t.$forceUpdate();
            }), ho(e, i);

            if ("in-out" === r) {
              if (xt(o)) return c;

              var p,
                  d = function d() {
                p();
              };

              vt(s, "afterEnter", d), vt(s, "enterCancelled", d), vt(f, "delayLeave", function (e) {
                p = e;
              });
            }
          }

          return i;
        }
      }
    },
        _o = L({
      tag: String,
      moveClass: String
    }, fo);

    delete _o.mode;
    var bo = {
      props: _o,
      beforeMount: function beforeMount() {
        var e = this,
            t = this._update;

        this._update = function (n, r) {
          var i = an(e);
          e.__patch__(e._vnode, e.kept, !1, !0), e._vnode = e.kept, i(), t.call(e, n, r);
        };
      },
      render: function render(e) {
        for (var t = this.tag || this.$vnode.data.tag || "span", n = Object.create(null), r = this.prevChildren = this.children, i = this.$slots["default"] || [], o = this.children = [], a = vo(this), s = 0; s < i.length; s++) {
          var c = i[s];
          if (c.tag) if (null != c.key && 0 !== String(c.key).indexOf("__vlist")) o.push(c), n[c.key] = c, (c.data || (c.data = {})).transition = a;else ;
        }

        if (r) {
          for (var u = [], l = [], f = 0; f < r.length; f++) {
            var p = r[f];
            p.data.transition = a, p.data.pos = p.elm.getBoundingClientRect(), n[p.key] ? u.push(p) : l.push(p);
          }

          this.kept = e(t, null, u), this.removed = l;
        }

        return e(t, null, o);
      },
      updated: function updated() {
        var e = this.prevChildren,
            t = this.moveClass || (this.name || "v") + "-move";
        e.length && this.hasMove(e[0].elm, t) && (e.forEach(wo), e.forEach(xo), e.forEach($o), this._reflow = document.body.offsetHeight, e.forEach(function (e) {
          if (e.data.moved) {
            var n = e.elm,
                r = n.style;
            Bi(n, t), r.transform = r.WebkitTransform = r.transitionDuration = "", n.addEventListener(Di, n._moveCb = function e(r) {
              r && r.target !== n || r && !/transform$/.test(r.propertyName) || (n.removeEventListener(Di, e), n._moveCb = null, Ui(n, t));
            });
          }
        }));
      },
      methods: {
        hasMove: function hasMove(e, t) {
          if (!Li) return !1;
          if (this._hasMove) return this._hasMove;
          var n = e.cloneNode();
          e._transitionClasses && e._transitionClasses.forEach(function (e) {
            Ti(n, e);
          }), Si(n, t), n.style.display = "none", this.$el.appendChild(n);
          var r = Ki(n);
          return this.$el.removeChild(n), this._hasMove = r.hasTransform;
        }
      }
    };

    function wo(e) {
      e.elm._moveCb && e.elm._moveCb(), e.elm._enterCb && e.elm._enterCb();
    }

    function xo(e) {
      e.data.newPos = e.elm.getBoundingClientRect();
    }

    function $o(e) {
      var t = e.data.pos,
          n = e.data.newPos,
          r = t.left - n.left,
          i = t.top - n.top;

      if (r || i) {
        e.data.moved = !0;
        var o = e.elm.style;
        o.transform = o.WebkitTransform = "translate(" + r + "px," + i + "px)", o.transitionDuration = "0s";
      }
    }

    var Co = {
      Transition: go,
      TransitionGroup: bo
    };
    Nn.config.mustUseProp = zn, Nn.config.isReservedTag = or, Nn.config.isReservedAttr = Un, Nn.config.getTagNamespace = ar, Nn.config.isUnknownElement = function (e) {
      if (!W) return !0;
      if (or(e)) return !1;
      if (e = e.toLowerCase(), null != sr[e]) return sr[e];
      var t = document.createElement(e);
      return e.indexOf("-") > -1 ? sr[e] = t.constructor === window.HTMLUnknownElement || t.constructor === window.HTMLElement : sr[e] = /HTMLUnknownElement/.test(t.toString());
    }, L(Nn.options.directives, lo), L(Nn.options.components, Co), Nn.prototype.__patch__ = W ? Qi : M, Nn.prototype.$mount = function (e, t) {
      return function (e, t, n) {
        var r;
        return e.$el = t, e.$options.render || (e.$options.render = we), ln(e, "beforeMount"), r = function r() {
          e._update(e._render(), n);
        }, new xn(e, r, M, {
          before: function before() {
            e._isMounted && !e._isDestroyed && ln(e, "beforeUpdate");
          }
        }, !0), n = !1, null == e.$vnode && (e._isMounted = !0, ln(e, "mounted")), e;
      }(this, e = e && W ? ur(e) : void 0, t);
    }, W && setTimeout(function () {
      V.devtools && ue && ue.emit("init", Nn);
    }, 0);
    var ko = /\{\{((?:.|\r?\n)+?)\}\}/g,
        Ao = /[-.*+?^${}()|[\]\/\\]/g,
        Oo = C(function (e) {
      var t = e[0].replace(Ao, "\\$&"),
          n = e[1].replace(Ao, "\\$&");
      return new RegExp(t + "((?:.|\\n)+?)" + n, "g");
    });
    var So = {
      staticKeys: ["staticClass"],
      transformNode: function transformNode(e, t) {
        t.warn;
        var n = Gr(e, "class");
        n && (e.staticClass = JSON.stringify(n));
        var r = qr(e, "class", !1);
        r && (e.classBinding = r);
      },
      genData: function genData(e) {
        var t = "";
        return e.staticClass && (t += "staticClass:" + e.staticClass + ","), e.classBinding && (t += "class:" + e.classBinding + ","), t;
      }
    };

    var To,
        Eo = {
      staticKeys: ["staticStyle"],
      transformNode: function transformNode(e, t) {
        t.warn;
        var n = Gr(e, "style");
        n && (e.staticStyle = JSON.stringify(mi(n)));
        var r = qr(e, "style", !1);
        r && (e.styleBinding = r);
      },
      genData: function genData(e) {
        var t = "";
        return e.staticStyle && (t += "staticStyle:" + e.staticStyle + ","), e.styleBinding && (t += "style:(" + e.styleBinding + "),"), t;
      }
    },
        jo = function jo(e) {
      return (To = To || document.createElement("div")).innerHTML = e, To.textContent;
    },
        Lo = g("area,base,br,col,embed,frame,hr,img,input,isindex,keygen,link,meta,param,source,track,wbr"),
        No = g("colgroup,dd,dt,li,options,p,td,tfoot,th,thead,tr,source"),
        Mo = g("address,article,aside,base,blockquote,body,caption,col,colgroup,dd,details,dialog,div,dl,dt,fieldset,figcaption,figure,footer,form,h1,h2,h3,h4,h5,h6,head,header,hgroup,hr,html,legend,li,menuitem,meta,optgroup,option,param,rp,rt,source,style,summary,tbody,td,tfoot,th,thead,title,tr,track"),
        Io = /^\s*([^\s"'<>\/=]+)(?:\s*(=)\s*(?:"([^"]*)"+|'([^']*)'+|([^\s"'=<>`]+)))?/,
        Do = /^\s*((?:v-[\w-]+:|@|:|#)\[[^=]+?\][^\s"'<>\/=]*)(?:\s*(=)\s*(?:"([^"]*)"+|'([^']*)'+|([^\s"'=<>`]+)))?/,
        Po = "[a-zA-Z_][\\-\\.0-9_a-zA-Z" + z.source + "]*",
        Fo = "((?:" + Po + "\\:)?" + Po + ")",
        Ro = new RegExp("^<" + Fo),
        Ho = /^\s*(\/?)>/,
        Bo = new RegExp("^<\\/" + Fo + "[^>]*>"),
        Uo = /^<!DOCTYPE [^>]+>/i,
        Vo = /^<!\--/,
        zo = /^<!\[/,
        Ko = g("script,style,textarea", !0),
        Jo = {},
        qo = {
      "&lt;": "<",
      "&gt;": ">",
      "&quot;": '"',
      "&amp;": "&",
      "&#10;": "\n",
      "&#9;": "\t",
      "&#39;": "'"
    },
        Go = /&(?:lt|gt|quot|amp|#39);/g,
        Zo = /&(?:lt|gt|quot|amp|#39|#10|#9);/g,
        Wo = g("pre,textarea", !0),
        Xo = function Xo(e, t) {
      return e && Wo(e) && "\n" === t[0];
    };

    function Yo(e, t) {
      var n = t ? Zo : Go;
      return e.replace(n, function (e) {
        return qo[e];
      });
    }

    var Qo,
        ea,
        ta,
        na,
        ra,
        ia,
        oa,
        aa,
        sa = /^@|^v-on:/,
        ca = /^v-|^@|^:|^#/,
        ua = /([\s\S]*?)\s+(?:in|of)\s+([\s\S]*)/,
        la = /,([^,\}\]]*)(?:,([^,\}\]]*))?$/,
        fa = /^\(|\)$/g,
        pa = /^\[.*\]$/,
        da = /:(.*)$/,
        va = /^:|^\.|^v-bind:/,
        ha = /\.[^.\]]+(?=[^\]]*$)/g,
        ma = /^v-slot(:|$)|^#/,
        ya = /[\r\n]/,
        ga = /[ \f\t\r\n]+/g,
        _a = C(jo),
        ba = "_empty_";

    function wa(e, t, n) {
      return {
        type: 1,
        tag: e,
        attrsList: t,
        attrsMap: Sa(t),
        rawAttrsMap: {},
        parent: n,
        children: []
      };
    }

    function xa(e, t) {
      Qo = t.warn || Rr, ia = t.isPreTag || I, oa = t.mustUseProp || I, aa = t.getTagNamespace || I;
      var n = t.isReservedTag || I;
      (function (e) {
        return !(!(e.component || e.attrsMap[":is"] || e.attrsMap["v-bind:is"]) && (e.attrsMap.is ? n(e.attrsMap.is) : n(e.tag)));
      }), ta = Hr(t.modules, "transformNode"), na = Hr(t.modules, "preTransformNode"), ra = Hr(t.modules, "postTransformNode"), ea = t.delimiters;
      var r,
          i,
          o = [],
          a = !1 !== t.preserveWhitespace,
          s = t.whitespace,
          c = !1,
          u = !1;

      function l(e) {
        if (f(e), c || e.processed || (e = $a(e, t)), o.length || e === r || r["if"] && (e.elseif || e["else"]) && ka(r, {
          exp: e.elseif,
          block: e
        }), i && !e.forbidden) if (e.elseif || e["else"]) a = e, s = function (e) {
          for (var t = e.length; t--;) {
            if (1 === e[t].type) return e[t];
            e.pop();
          }
        }(i.children), s && s["if"] && ka(s, {
          exp: a.elseif,
          block: a
        });else {
          if (e.slotScope) {
            var n = e.slotTarget || '"default"';
            (i.scopedSlots || (i.scopedSlots = {}))[n] = e;
          }

          i.children.push(e), e.parent = i;
        }
        var a, s;
        e.children = e.children.filter(function (e) {
          return !e.slotScope;
        }), f(e), e.pre && (c = !1), ia(e.tag) && (u = !1);

        for (var l = 0; l < ra.length; l++) {
          ra[l](e, t);
        }
      }

      function f(e) {
        if (!u) for (var t; (t = e.children[e.children.length - 1]) && 3 === t.type && " " === t.text;) {
          e.children.pop();
        }
      }

      return function (e, t) {
        for (var n, r, i = [], o = t.expectHTML, a = t.isUnaryTag || I, s = t.canBeLeftOpenTag || I, c = 0; e;) {
          if (n = e, r && Ko(r)) {
            var u = 0,
                l = r.toLowerCase(),
                f = Jo[l] || (Jo[l] = new RegExp("([\\s\\S]*?)(</" + l + "[^>]*>)", "i")),
                p = e.replace(f, function (e, n, r) {
              return u = r.length, Ko(l) || "noscript" === l || (n = n.replace(/<!\--([\s\S]*?)-->/g, "$1").replace(/<!\[CDATA\[([\s\S]*?)]]>/g, "$1")), Xo(l, n) && (n = n.slice(1)), t.chars && t.chars(n), "";
            });
            c += e.length - p.length, e = p, A(l, c - u, c);
          } else {
            var d = e.indexOf("<");

            if (0 === d) {
              if (Vo.test(e)) {
                var v = e.indexOf("--\x3e");

                if (v >= 0) {
                  t.shouldKeepComment && t.comment(e.substring(4, v), c, c + v + 3), $(v + 3);
                  continue;
                }
              }

              if (zo.test(e)) {
                var h = e.indexOf("]>");

                if (h >= 0) {
                  $(h + 2);
                  continue;
                }
              }

              var m = e.match(Uo);

              if (m) {
                $(m[0].length);
                continue;
              }

              var y = e.match(Bo);

              if (y) {
                var g = c;
                $(y[0].length), A(y[1], g, c);
                continue;
              }

              var _ = C();

              if (_) {
                k(_), Xo(_.tagName, e) && $(1);
                continue;
              }
            }

            var b = void 0,
                w = void 0,
                x = void 0;

            if (d >= 0) {
              for (w = e.slice(d); !(Bo.test(w) || Ro.test(w) || Vo.test(w) || zo.test(w) || (x = w.indexOf("<", 1)) < 0);) {
                d += x, w = e.slice(d);
              }

              b = e.substring(0, d);
            }

            d < 0 && (b = e), b && $(b.length), t.chars && b && t.chars(b, c - b.length, c);
          }

          if (e === n) {
            t.chars && t.chars(e);
            break;
          }
        }

        function $(t) {
          c += t, e = e.substring(t);
        }

        function C() {
          var t = e.match(Ro);

          if (t) {
            var n,
                r,
                i = {
              tagName: t[1],
              attrs: [],
              start: c
            };

            for ($(t[0].length); !(n = e.match(Ho)) && (r = e.match(Do) || e.match(Io));) {
              r.start = c, $(r[0].length), r.end = c, i.attrs.push(r);
            }

            if (n) return i.unarySlash = n[1], $(n[0].length), i.end = c, i;
          }
        }

        function k(e) {
          var n = e.tagName,
              c = e.unarySlash;
          o && ("p" === r && Mo(n) && A(r), s(n) && r === n && A(n));

          for (var u = a(n) || !!c, l = e.attrs.length, f = new Array(l), p = 0; p < l; p++) {
            var d = e.attrs[p],
                v = d[3] || d[4] || d[5] || "",
                h = "a" === n && "href" === d[1] ? t.shouldDecodeNewlinesForHref : t.shouldDecodeNewlines;
            f[p] = {
              name: d[1],
              value: Yo(v, h)
            };
          }

          u || (i.push({
            tag: n,
            lowerCasedTag: n.toLowerCase(),
            attrs: f,
            start: e.start,
            end: e.end
          }), r = n), t.start && t.start(n, f, u, e.start, e.end);
        }

        function A(e, n, o) {
          var a, s;
          if (null == n && (n = c), null == o && (o = c), e) for (s = e.toLowerCase(), a = i.length - 1; a >= 0 && i[a].lowerCasedTag !== s; a--) {
            ;
          } else a = 0;

          if (a >= 0) {
            for (var u = i.length - 1; u >= a; u--) {
              t.end && t.end(i[u].tag, n, o);
            }

            i.length = a, r = a && i[a - 1].tag;
          } else "br" === s ? t.start && t.start(e, [], !0, n, o) : "p" === s && (t.start && t.start(e, [], !1, n, o), t.end && t.end(e, n, o));
        }

        A();
      }(e, {
        warn: Qo,
        expectHTML: t.expectHTML,
        isUnaryTag: t.isUnaryTag,
        canBeLeftOpenTag: t.canBeLeftOpenTag,
        shouldDecodeNewlines: t.shouldDecodeNewlines,
        shouldDecodeNewlinesForHref: t.shouldDecodeNewlinesForHref,
        shouldKeepComment: t.comments,
        outputSourceRange: t.outputSourceRange,
        start: function start(e, n, a, s, f) {
          var p = i && i.ns || aa(e);
          ee && "svg" === p && (n = function (e) {
            for (var t = [], n = 0; n < e.length; n++) {
              var r = e[n];
              Ta.test(r.name) || (r.name = r.name.replace(Ea, ""), t.push(r));
            }

            return t;
          }(n));
          var d,
              v = wa(e, n, i);
          p && (v.ns = p), "style" !== (d = v).tag && ("script" !== d.tag || d.attrsMap.type && "text/javascript" !== d.attrsMap.type) || ce() || (v.forbidden = !0);

          for (var h = 0; h < na.length; h++) {
            v = na[h](v, t) || v;
          }

          c || (!function (e) {
            null != Gr(e, "v-pre") && (e.pre = !0);
          }(v), v.pre && (c = !0)), ia(v.tag) && (u = !0), c ? function (e) {
            var t = e.attrsList,
                n = t.length;
            if (n) for (var r = e.attrs = new Array(n), i = 0; i < n; i++) {
              r[i] = {
                name: t[i].name,
                value: JSON.stringify(t[i].value)
              }, null != t[i].start && (r[i].start = t[i].start, r[i].end = t[i].end);
            } else e.pre || (e.plain = !0);
          }(v) : v.processed || (Ca(v), function (e) {
            var t = Gr(e, "v-if");
            if (t) e["if"] = t, ka(e, {
              exp: t,
              block: e
            });else {
              null != Gr(e, "v-else") && (e["else"] = !0);
              var n = Gr(e, "v-else-if");
              n && (e.elseif = n);
            }
          }(v), function (e) {
            null != Gr(e, "v-once") && (e.once = !0);
          }(v)), r || (r = v), a ? l(v) : (i = v, o.push(v));
        },
        end: function end(e, t, n) {
          var r = o[o.length - 1];
          o.length -= 1, i = o[o.length - 1], l(r);
        },
        chars: function chars(e, t, n) {
          if (i && (!ee || "textarea" !== i.tag || i.attrsMap.placeholder !== e)) {
            var r,
                o,
                l,
                f = i.children;
            if (e = u || e.trim() ? "script" === (r = i).tag || "style" === r.tag ? e : _a(e) : f.length ? s ? "condense" === s && ya.test(e) ? "" : " " : a ? " " : "" : "") u || "condense" !== s || (e = e.replace(ga, " ")), !c && " " !== e && (o = function (e, t) {
              var n = t ? Oo(t) : ko;

              if (n.test(e)) {
                for (var r, i, o, a = [], s = [], c = n.lastIndex = 0; r = n.exec(e);) {
                  (i = r.index) > c && (s.push(o = e.slice(c, i)), a.push(JSON.stringify(o)));
                  var u = Pr(r[1].trim());
                  a.push("_s(" + u + ")"), s.push({
                    "@binding": u
                  }), c = i + r[0].length;
                }

                return c < e.length && (s.push(o = e.slice(c)), a.push(JSON.stringify(o))), {
                  expression: a.join("+"),
                  tokens: s
                };
              }
            }(e, ea)) ? l = {
              type: 2,
              expression: o.expression,
              tokens: o.tokens,
              text: e
            } : " " === e && f.length && " " === f[f.length - 1].text || (l = {
              type: 3,
              text: e
            }), l && f.push(l);
          }
        },
        comment: function comment(e, t, n) {
          if (i) {
            var r = {
              type: 3,
              text: e,
              isComment: !0
            };
            0, i.children.push(r);
          }
        }
      }), r;
    }

    function $a(e, t) {
      var n;
      !function (e) {
        var t = qr(e, "key");

        if (t) {
          e.key = t;
        }
      }(e), e.plain = !e.key && !e.scopedSlots && !e.attrsList.length, function (e) {
        var t = qr(e, "ref");
        t && (e.ref = t, e.refInFor = function (e) {
          var t = e;

          for (; t;) {
            if (void 0 !== t["for"]) return !0;
            t = t.parent;
          }

          return !1;
        }(e));
      }(e), function (e) {
        var t;
        "template" === e.tag ? (t = Gr(e, "scope"), e.slotScope = t || Gr(e, "slot-scope")) : (t = Gr(e, "slot-scope")) && (e.slotScope = t);
        var n = qr(e, "slot");
        n && (e.slotTarget = '""' === n ? '"default"' : n, e.slotTargetDynamic = !(!e.attrsMap[":slot"] && !e.attrsMap["v-bind:slot"]), "template" === e.tag || e.slotScope || Ur(e, "slot", n, function (e, t) {
          return e.rawAttrsMap[":" + t] || e.rawAttrsMap["v-bind:" + t] || e.rawAttrsMap[t];
        }(e, "slot")));

        if ("template" === e.tag) {
          var r = Zr(e, ma);

          if (r) {
            0;
            var i = Aa(r),
                o = i.name,
                a = i.dynamic;
            e.slotTarget = o, e.slotTargetDynamic = a, e.slotScope = r.value || ba;
          }
        } else {
          var s = Zr(e, ma);

          if (s) {
            0;
            var c = e.scopedSlots || (e.scopedSlots = {}),
                u = Aa(s),
                l = u.name,
                f = u.dynamic,
                p = c[l] = wa("template", [], e);
            p.slotTarget = l, p.slotTargetDynamic = f, p.children = e.children.filter(function (e) {
              if (!e.slotScope) return e.parent = p, !0;
            }), p.slotScope = s.value || ba, e.children = [], e.plain = !1;
          }
        }
      }(e), "slot" === (n = e).tag && (n.slotName = qr(n, "name")), function (e) {
        var t;
        (t = qr(e, "is")) && (e.component = t);
        null != Gr(e, "inline-template") && (e.inlineTemplate = !0);
      }(e);

      for (var r = 0; r < ta.length; r++) {
        e = ta[r](e, t) || e;
      }

      return function (e) {
        var t,
            n,
            r,
            i,
            o,
            a,
            s,
            c,
            u = e.attrsList;

        for (t = 0, n = u.length; t < n; t++) {
          if (r = i = u[t].name, o = u[t].value, ca.test(r)) {
            if (e.hasBindings = !0, (a = Oa(r.replace(ca, ""))) && (r = r.replace(ha, "")), va.test(r)) r = r.replace(va, ""), o = Pr(o), (c = pa.test(r)) && (r = r.slice(1, -1)), a && (a.prop && !c && "innerHtml" === (r = A(r)) && (r = "innerHTML"), a.camel && !c && (r = A(r)), a.sync && (s = Yr(o, "$event"), c ? Jr(e, '"update:"+(' + r + ")", s, null, !1, 0, u[t], !0) : (Jr(e, "update:" + A(r), s, null, !1, 0, u[t]), T(r) !== A(r) && Jr(e, "update:" + T(r), s, null, !1, 0, u[t])))), a && a.prop || !e.component && oa(e.tag, e.attrsMap.type, r) ? Br(e, r, o, u[t], c) : Ur(e, r, o, u[t], c);else if (sa.test(r)) r = r.replace(sa, ""), (c = pa.test(r)) && (r = r.slice(1, -1)), Jr(e, r, o, a, !1, 0, u[t], c);else {
              var l = (r = r.replace(ca, "")).match(da),
                  f = l && l[1];
              c = !1, f && (r = r.slice(0, -(f.length + 1)), pa.test(f) && (f = f.slice(1, -1), c = !0)), zr(e, r, i, o, f, c, a, u[t]);
            }
          } else Ur(e, r, JSON.stringify(o), u[t]), !e.component && "muted" === r && oa(e.tag, e.attrsMap.type, r) && Br(e, r, "true", u[t]);
        }
      }(e), e;
    }

    function Ca(e) {
      var t;

      if (t = Gr(e, "v-for")) {
        var n = function (e) {
          var t = e.match(ua);
          if (!t) return;
          var n = {};
          n["for"] = t[2].trim();
          var r = t[1].trim().replace(fa, ""),
              i = r.match(la);
          i ? (n.alias = r.replace(la, "").trim(), n.iterator1 = i[1].trim(), i[2] && (n.iterator2 = i[2].trim())) : n.alias = r;
          return n;
        }(t);

        n && L(e, n);
      }
    }

    function ka(e, t) {
      e.ifConditions || (e.ifConditions = []), e.ifConditions.push(t);
    }

    function Aa(e) {
      var t = e.name.replace(ma, "");
      return t || "#" !== e.name[0] && (t = "default"), pa.test(t) ? {
        name: t.slice(1, -1),
        dynamic: !0
      } : {
        name: '"' + t + '"',
        dynamic: !1
      };
    }

    function Oa(e) {
      var t = e.match(ha);

      if (t) {
        var n = {};
        return t.forEach(function (e) {
          n[e.slice(1)] = !0;
        }), n;
      }
    }

    function Sa(e) {
      for (var t = {}, n = 0, r = e.length; n < r; n++) {
        t[e[n].name] = e[n].value;
      }

      return t;
    }

    var Ta = /^xmlns:NS\d+/,
        Ea = /^NS\d+:/;

    function ja(e) {
      return wa(e.tag, e.attrsList.slice(), e.parent);
    }

    var La = {
      preTransformNode: function preTransformNode(e, t) {
        if ("input" === e.tag) {
          var n,
              r = e.attrsMap;
          if (!r["v-model"]) return;

          if ((r[":type"] || r["v-bind:type"]) && (n = qr(e, "type")), r.type || n || !r["v-bind"] || (n = "(" + r["v-bind"] + ").type"), n) {
            var i = Gr(e, "v-if", !0),
                o = i ? "&&(" + i + ")" : "",
                a = null != Gr(e, "v-else", !0),
                s = Gr(e, "v-else-if", !0),
                c = ja(e);
            Ca(c), Vr(c, "type", "checkbox"), $a(c, t), c.processed = !0, c["if"] = "(" + n + ")==='checkbox'" + o, ka(c, {
              exp: c["if"],
              block: c
            });
            var u = ja(e);
            Gr(u, "v-for", !0), Vr(u, "type", "radio"), $a(u, t), ka(c, {
              exp: "(" + n + ")==='radio'" + o,
              block: u
            });
            var l = ja(e);
            return Gr(l, "v-for", !0), Vr(l, ":type", n), $a(l, t), ka(c, {
              exp: i,
              block: l
            }), a ? c["else"] = !0 : s && (c.elseif = s), c;
          }
        }
      }
    },
        Na = [So, Eo, La];
    var Ma,
        Ia,
        Da = {
      model: function model(e, t, n) {
        n;
        var r = t.value,
            i = t.modifiers,
            o = e.tag,
            a = e.attrsMap.type;
        if (e.component) return Xr(e, r, i), !1;
        if ("select" === o) !function (e, t, n) {
          var r = 'var $$selectedVal = Array.prototype.filter.call($event.target.options,function(o){return o.selected}).map(function(o){var val = "_value" in o ? o._value : o.value;return ' + (n && n.number ? "_n(val)" : "val") + "});";
          r = r + " " + Yr(t, "$event.target.multiple ? $$selectedVal : $$selectedVal[0]"), Jr(e, "change", r, null, !0);
        }(e, r, i);else if ("input" === o && "checkbox" === a) !function (e, t, n) {
          var r = n && n.number,
              i = qr(e, "value") || "null",
              o = qr(e, "true-value") || "true",
              a = qr(e, "false-value") || "false";
          Br(e, "checked", "Array.isArray(" + t + ")?_i(" + t + "," + i + ")>-1" + ("true" === o ? ":(" + t + ")" : ":_q(" + t + "," + o + ")")), Jr(e, "change", "var $$a=" + t + ",$$el=$event.target,$$c=$$el.checked?(" + o + "):(" + a + ");if(Array.isArray($$a)){var $$v=" + (r ? "_n(" + i + ")" : i) + ",$$i=_i($$a,$$v);if($$el.checked){$$i<0&&(" + Yr(t, "$$a.concat([$$v])") + ")}else{$$i>-1&&(" + Yr(t, "$$a.slice(0,$$i).concat($$a.slice($$i+1))") + ")}}else{" + Yr(t, "$$c") + "}", null, !0);
        }(e, r, i);else if ("input" === o && "radio" === a) !function (e, t, n) {
          var r = n && n.number,
              i = qr(e, "value") || "null";
          Br(e, "checked", "_q(" + t + "," + (i = r ? "_n(" + i + ")" : i) + ")"), Jr(e, "change", Yr(t, i), null, !0);
        }(e, r, i);else if ("input" === o || "textarea" === o) !function (e, t, n) {
          var r = e.attrsMap.type;
          0;
          var i = n || {},
              o = i.lazy,
              a = i.number,
              s = i.trim,
              c = !o && "range" !== r,
              u = o ? "change" : "range" === r ? oi : "input",
              l = "$event.target.value";
          s && (l = "$event.target.value.trim()");
          a && (l = "_n(" + l + ")");
          var f = Yr(t, l);
          c && (f = "if($event.target.composing)return;" + f);
          Br(e, "value", "(" + t + ")"), Jr(e, u, f, null, !0), (s || a) && Jr(e, "blur", "$forceUpdate()");
        }(e, r, i);else {
          if (!V.isReservedTag(o)) return Xr(e, r, i), !1;
        }
        return !0;
      },
      text: function text(e, t) {
        t.value && Br(e, "textContent", "_s(" + t.value + ")", t);
      },
      html: function html(e, t) {
        t.value && Br(e, "innerHTML", "_s(" + t.value + ")", t);
      }
    },
        Pa = {
      expectHTML: !0,
      modules: Na,
      directives: Da,
      isPreTag: function isPreTag(e) {
        return "pre" === e;
      },
      isUnaryTag: Lo,
      mustUseProp: zn,
      canBeLeftOpenTag: No,
      isReservedTag: or,
      getTagNamespace: ar,
      staticKeys: function (e) {
        return e.reduce(function (e, t) {
          return e.concat(t.staticKeys || []);
        }, []).join(",");
      }(Na)
    },
        Fa = C(function (e) {
      return g("type,tag,attrsList,attrsMap,plain,parent,children,attrs,start,end,rawAttrsMap" + (e ? "," + e : ""));
    });

    function Ra(e, t) {
      e && (Ma = Fa(t.staticKeys || ""), Ia = t.isReservedTag || I, Ha(e), Ba(e, !1));
    }

    function Ha(e) {
      if (e["static"] = function (e) {
        if (2 === e.type) return !1;
        if (3 === e.type) return !0;
        return !(!e.pre && (e.hasBindings || e["if"] || e["for"] || _(e.tag) || !Ia(e.tag) || function (e) {
          for (; e.parent;) {
            if ("template" !== (e = e.parent).tag) return !1;
            if (e["for"]) return !0;
          }

          return !1;
        }(e) || !Object.keys(e).every(Ma)));
      }(e), 1 === e.type) {
        if (!Ia(e.tag) && "slot" !== e.tag && null == e.attrsMap["inline-template"]) return;

        for (var t = 0, n = e.children.length; t < n; t++) {
          var r = e.children[t];
          Ha(r), r["static"] || (e["static"] = !1);
        }

        if (e.ifConditions) for (var i = 1, o = e.ifConditions.length; i < o; i++) {
          var a = e.ifConditions[i].block;
          Ha(a), a["static"] || (e["static"] = !1);
        }
      }
    }

    function Ba(e, t) {
      if (1 === e.type) {
        if ((e["static"] || e.once) && (e.staticInFor = t), e["static"] && e.children.length && (1 !== e.children.length || 3 !== e.children[0].type)) return void (e.staticRoot = !0);
        if (e.staticRoot = !1, e.children) for (var n = 0, r = e.children.length; n < r; n++) {
          Ba(e.children[n], t || !!e["for"]);
        }
        if (e.ifConditions) for (var i = 1, o = e.ifConditions.length; i < o; i++) {
          Ba(e.ifConditions[i].block, t);
        }
      }
    }

    var Ua = /^([\w$_]+|\([^)]*?\))\s*=>|^function(?:\s+[\w$]+)?\s*\(/,
        Va = /\([^)]*?\);*$/,
        za = /^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*|\['[^']*?']|\["[^"]*?"]|\[\d+]|\[[A-Za-z_$][\w$]*])*$/,
        Ka = {
      esc: 27,
      tab: 9,
      enter: 13,
      space: 32,
      up: 38,
      left: 37,
      right: 39,
      down: 40,
      "delete": [8, 46]
    },
        Ja = {
      esc: ["Esc", "Escape"],
      tab: "Tab",
      enter: "Enter",
      space: [" ", "Spacebar"],
      up: ["Up", "ArrowUp"],
      left: ["Left", "ArrowLeft"],
      right: ["Right", "ArrowRight"],
      down: ["Down", "ArrowDown"],
      "delete": ["Backspace", "Delete", "Del"]
    },
        qa = function qa(e) {
      return "if(" + e + ")return null;";
    },
        Ga = {
      stop: "$event.stopPropagation();",
      prevent: "$event.preventDefault();",
      self: qa("$event.target !== $event.currentTarget"),
      ctrl: qa("!$event.ctrlKey"),
      shift: qa("!$event.shiftKey"),
      alt: qa("!$event.altKey"),
      meta: qa("!$event.metaKey"),
      left: qa("'button' in $event && $event.button !== 0"),
      middle: qa("'button' in $event && $event.button !== 1"),
      right: qa("'button' in $event && $event.button !== 2")
    };

    function Za(e, t) {
      var n = t ? "nativeOn:" : "on:",
          r = "",
          i = "";

      for (var o in e) {
        var a = Wa(e[o]);
        e[o] && e[o].dynamic ? i += o + "," + a + "," : r += '"' + o + '":' + a + ",";
      }

      return r = "{" + r.slice(0, -1) + "}", i ? n + "_d(" + r + ",[" + i.slice(0, -1) + "])" : n + r;
    }

    function Wa(e) {
      if (!e) return "function(){}";
      if (Array.isArray(e)) return "[" + e.map(function (e) {
        return Wa(e);
      }).join(",") + "]";
      var t = za.test(e.value),
          n = Ua.test(e.value),
          r = za.test(e.value.replace(Va, ""));

      if (e.modifiers) {
        var i = "",
            o = "",
            a = [];

        for (var s in e.modifiers) {
          if (Ga[s]) o += Ga[s], Ka[s] && a.push(s);else if ("exact" === s) {
            var c = e.modifiers;
            o += qa(["ctrl", "shift", "alt", "meta"].filter(function (e) {
              return !c[e];
            }).map(function (e) {
              return "$event." + e + "Key";
            }).join("||"));
          } else a.push(s);
        }

        return a.length && (i += function (e) {
          return "if(!$event.type.indexOf('key')&&" + e.map(Xa).join("&&") + ")return null;";
        }(a)), o && (i += o), "function($event){" + i + (t ? "return " + e.value + ".apply(null, arguments)" : n ? "return (" + e.value + ").apply(null, arguments)" : r ? "return " + e.value : e.value) + "}";
      }

      return t || n ? e.value : "function($event){" + (r ? "return " + e.value : e.value) + "}";
    }

    function Xa(e) {
      var t = parseInt(e, 10);
      if (t) return "$event.keyCode!==" + t;
      var n = Ka[e],
          r = Ja[e];
      return "_k($event.keyCode," + JSON.stringify(e) + "," + JSON.stringify(n) + ",$event.key," + JSON.stringify(r) + ")";
    }

    var Ya = {
      on: function on(e, t) {
        e.wrapListeners = function (e) {
          return "_g(" + e + "," + t.value + ")";
        };
      },
      bind: function bind(e, t) {
        e.wrapData = function (n) {
          return "_b(" + n + ",'" + e.tag + "'," + t.value + "," + (t.modifiers && t.modifiers.prop ? "true" : "false") + (t.modifiers && t.modifiers.sync ? ",true" : "") + ")";
        };
      },
      cloak: M
    },
        Qa = function Qa(e) {
      this.options = e, this.warn = e.warn || Rr, this.transforms = Hr(e.modules, "transformCode"), this.dataGenFns = Hr(e.modules, "genData"), this.directives = L(L({}, Ya), e.directives);
      var t = e.isReservedTag || I;
      this.maybeComponent = function (e) {
        return !!e.component || !t(e.tag);
      }, this.onceId = 0, this.staticRenderFns = [], this.pre = !1;
    };

    function es(e, t) {
      var n = new Qa(t);
      return {
        render: "with(this){return " + (e ? "script" === e.tag ? "null" : ts(e, n) : '_c("div")') + "}",
        staticRenderFns: n.staticRenderFns
      };
    }

    function ts(e, t) {
      if (e.parent && (e.pre = e.pre || e.parent.pre), e.staticRoot && !e.staticProcessed) return ns(e, t);
      if (e.once && !e.onceProcessed) return rs(e, t);
      if (e["for"] && !e.forProcessed) return as(e, t);
      if (e["if"] && !e.ifProcessed) return is(e, t);

      if ("template" !== e.tag || e.slotTarget || t.pre) {
        if ("slot" === e.tag) return function (e, t) {
          var n = e.slotName || '"default"',
              r = ls(e, t),
              i = "_t(" + n + (r ? ",function(){return " + r + "}" : ""),
              o = e.attrs || e.dynamicAttrs ? ds((e.attrs || []).concat(e.dynamicAttrs || []).map(function (e) {
            return {
              name: A(e.name),
              value: e.value,
              dynamic: e.dynamic
            };
          })) : null,
              a = e.attrsMap["v-bind"];
          !o && !a || r || (i += ",null");
          o && (i += "," + o);
          a && (i += (o ? "" : ",null") + "," + a);
          return i + ")";
        }(e, t);
        var n;
        if (e.component) n = function (e, t, n) {
          var r = t.inlineTemplate ? null : ls(t, n, !0);
          return "_c(" + e + "," + ss(t, n) + (r ? "," + r : "") + ")";
        }(e.component, e, t);else {
          var r;
          (!e.plain || e.pre && t.maybeComponent(e)) && (r = ss(e, t));
          var i = e.inlineTemplate ? null : ls(e, t, !0);
          n = "_c('" + e.tag + "'" + (r ? "," + r : "") + (i ? "," + i : "") + ")";
        }

        for (var o = 0; o < t.transforms.length; o++) {
          n = t.transforms[o](e, n);
        }

        return n;
      }

      return ls(e, t) || "void 0";
    }

    function ns(e, t) {
      e.staticProcessed = !0;
      var n = t.pre;
      return e.pre && (t.pre = e.pre), t.staticRenderFns.push("with(this){return " + ts(e, t) + "}"), t.pre = n, "_m(" + (t.staticRenderFns.length - 1) + (e.staticInFor ? ",true" : "") + ")";
    }

    function rs(e, t) {
      if (e.onceProcessed = !0, e["if"] && !e.ifProcessed) return is(e, t);

      if (e.staticInFor) {
        for (var n = "", r = e.parent; r;) {
          if (r["for"]) {
            n = r.key;
            break;
          }

          r = r.parent;
        }

        return n ? "_o(" + ts(e, t) + "," + t.onceId++ + "," + n + ")" : ts(e, t);
      }

      return ns(e, t);
    }

    function is(e, t, n, r) {
      return e.ifProcessed = !0, os(e.ifConditions.slice(), t, n, r);
    }

    function os(e, t, n, r) {
      if (!e.length) return r || "_e()";
      var i = e.shift();
      return i.exp ? "(" + i.exp + ")?" + o(i.block) + ":" + os(e, t, n, r) : "" + o(i.block);

      function o(e) {
        return n ? n(e, t) : e.once ? rs(e, t) : ts(e, t);
      }
    }

    function as(e, t, n, r) {
      var i = e["for"],
          o = e.alias,
          a = e.iterator1 ? "," + e.iterator1 : "",
          s = e.iterator2 ? "," + e.iterator2 : "";
      return e.forProcessed = !0, (r || "_l") + "((" + i + "),function(" + o + a + s + "){return " + (n || ts)(e, t) + "})";
    }

    function ss(e, t) {
      var n = "{",
          r = function (e, t) {
        var n = e.directives;
        if (!n) return;
        var r,
            i,
            o,
            a,
            s = "directives:[",
            c = !1;

        for (r = 0, i = n.length; r < i; r++) {
          o = n[r], a = !0;
          var u = t.directives[o.name];
          u && (a = !!u(e, o, t.warn)), a && (c = !0, s += '{name:"' + o.name + '",rawName:"' + o.rawName + '"' + (o.value ? ",value:(" + o.value + "),expression:" + JSON.stringify(o.value) : "") + (o.arg ? ",arg:" + (o.isDynamicArg ? o.arg : '"' + o.arg + '"') : "") + (o.modifiers ? ",modifiers:" + JSON.stringify(o.modifiers) : "") + "},");
        }

        if (c) return s.slice(0, -1) + "]";
      }(e, t);

      r && (n += r + ","), e.key && (n += "key:" + e.key + ","), e.ref && (n += "ref:" + e.ref + ","), e.refInFor && (n += "refInFor:true,"), e.pre && (n += "pre:true,"), e.component && (n += 'tag:"' + e.tag + '",');

      for (var i = 0; i < t.dataGenFns.length; i++) {
        n += t.dataGenFns[i](e);
      }

      if (e.attrs && (n += "attrs:" + ds(e.attrs) + ","), e.props && (n += "domProps:" + ds(e.props) + ","), e.events && (n += Za(e.events, !1) + ","), e.nativeEvents && (n += Za(e.nativeEvents, !0) + ","), e.slotTarget && !e.slotScope && (n += "slot:" + e.slotTarget + ","), e.scopedSlots && (n += function (e, t, n) {
        var r = e["for"] || Object.keys(t).some(function (e) {
          var n = t[e];
          return n.slotTargetDynamic || n["if"] || n["for"] || cs(n);
        }),
            i = !!e["if"];
        if (!r) for (var o = e.parent; o;) {
          if (o.slotScope && o.slotScope !== ba || o["for"]) {
            r = !0;
            break;
          }

          o["if"] && (i = !0), o = o.parent;
        }
        var a = Object.keys(t).map(function (e) {
          return us(t[e], n);
        }).join(",");
        return "scopedSlots:_u([" + a + "]" + (r ? ",null,true" : "") + (!r && i ? ",null,false," + function (e) {
          var t = 5381,
              n = e.length;

          for (; n;) {
            t = 33 * t ^ e.charCodeAt(--n);
          }

          return t >>> 0;
        }(a) : "") + ")";
      }(e, e.scopedSlots, t) + ","), e.model && (n += "model:{value:" + e.model.value + ",callback:" + e.model.callback + ",expression:" + e.model.expression + "},"), e.inlineTemplate) {
        var o = function (e, t) {
          var n = e.children[0];
          0;

          if (n && 1 === n.type) {
            var r = es(n, t.options);
            return "inlineTemplate:{render:function(){" + r.render + "},staticRenderFns:[" + r.staticRenderFns.map(function (e) {
              return "function(){" + e + "}";
            }).join(",") + "]}";
          }
        }(e, t);

        o && (n += o + ",");
      }

      return n = n.replace(/,$/, "") + "}", e.dynamicAttrs && (n = "_b(" + n + ',"' + e.tag + '",' + ds(e.dynamicAttrs) + ")"), e.wrapData && (n = e.wrapData(n)), e.wrapListeners && (n = e.wrapListeners(n)), n;
    }

    function cs(e) {
      return 1 === e.type && ("slot" === e.tag || e.children.some(cs));
    }

    function us(e, t) {
      var n = e.attrsMap["slot-scope"];
      if (e["if"] && !e.ifProcessed && !n) return is(e, t, us, "null");
      if (e["for"] && !e.forProcessed) return as(e, t, us);
      var r = e.slotScope === ba ? "" : String(e.slotScope),
          i = "function(" + r + "){return " + ("template" === e.tag ? e["if"] && n ? "(" + e["if"] + ")?" + (ls(e, t) || "undefined") + ":undefined" : ls(e, t) || "undefined" : ts(e, t)) + "}",
          o = r ? "" : ",proxy:true";
      return "{key:" + (e.slotTarget || '"default"') + ",fn:" + i + o + "}";
    }

    function ls(e, t, n, r, i) {
      var o = e.children;

      if (o.length) {
        var a = o[0];

        if (1 === o.length && a["for"] && "template" !== a.tag && "slot" !== a.tag) {
          var s = n ? t.maybeComponent(a) ? ",1" : ",0" : "";
          return "" + (r || ts)(a, t) + s;
        }

        var c = n ? function (e, t) {
          for (var n = 0, r = 0; r < e.length; r++) {
            var i = e[r];

            if (1 === i.type) {
              if (fs(i) || i.ifConditions && i.ifConditions.some(function (e) {
                return fs(e.block);
              })) {
                n = 2;
                break;
              }

              (t(i) || i.ifConditions && i.ifConditions.some(function (e) {
                return t(e.block);
              })) && (n = 1);
            }
          }

          return n;
        }(o, t.maybeComponent) : 0,
            u = i || ps;
        return "[" + o.map(function (e) {
          return u(e, t);
        }).join(",") + "]" + (c ? "," + c : "");
      }
    }

    function fs(e) {
      return void 0 !== e["for"] || "template" === e.tag || "slot" === e.tag;
    }

    function ps(e, t) {
      return 1 === e.type ? ts(e, t) : 3 === e.type && e.isComment ? function (e) {
        return "_e(" + JSON.stringify(e.text) + ")";
      }(e) : "_v(" + (2 === (n = e).type ? n.expression : vs(JSON.stringify(n.text))) + ")";
      var n;
    }

    function ds(e) {
      for (var t = "", n = "", r = 0; r < e.length; r++) {
        var i = e[r],
            o = vs(i.value);
        i.dynamic ? n += i.name + "," + o + "," : t += '"' + i.name + '":' + o + ",";
      }

      return t = "{" + t.slice(0, -1) + "}", n ? "_d(" + t + ",[" + n.slice(0, -1) + "])" : t;
    }

    function vs(e) {
      return e.replace(/\u2028/g, "\\u2028").replace(/\u2029/g, "\\u2029");
    }

    new RegExp("\\b" + "do,if,for,let,new,try,var,case,else,with,await,break,catch,class,const,super,throw,while,yield,delete,export,import,return,switch,default,extends,finally,continue,debugger,function,arguments".split(",").join("\\b|\\b") + "\\b"), new RegExp("\\b" + "delete,typeof,void".split(",").join("\\s*\\([^\\)]*\\)|\\b") + "\\s*\\([^\\)]*\\)");

    function hs(e, t) {
      try {
        return new Function(e);
      } catch (n) {
        return t.push({
          err: n,
          code: e
        }), M;
      }
    }

    function ms(e) {
      var t = Object.create(null);
      return function (n, r, i) {
        (r = L({}, r)).warn;
        delete r.warn;
        var o = r.delimiters ? String(r.delimiters) + n : n;
        if (t[o]) return t[o];
        var a = e(n, r);
        var s = {},
            c = [];
        return s.render = hs(a.render, c), s.staticRenderFns = a.staticRenderFns.map(function (e) {
          return hs(e, c);
        }), t[o] = s;
      };
    }

    var ys,
        gs,
        _s = (ys = function ys(e, t) {
      var n = xa(e.trim(), t);
      !1 !== t.optimize && Ra(n, t);
      var r = es(n, t);
      return {
        ast: n,
        render: r.render,
        staticRenderFns: r.staticRenderFns
      };
    }, function (e) {
      function t(t, n) {
        var r = Object.create(e),
            i = [],
            o = [];
        if (n) for (var a in n.modules && (r.modules = (e.modules || []).concat(n.modules)), n.directives && (r.directives = L(Object.create(e.directives || null), n.directives)), n) {
          "modules" !== a && "directives" !== a && (r[a] = n[a]);
        }

        r.warn = function (e, t, n) {
          (n ? o : i).push(e);
        };

        var s = ys(t.trim(), r);
        return s.errors = i, s.tips = o, s;
      }

      return {
        compile: t,
        compileToFunctions: ms(t)
      };
    }),
        bs = _s(Pa),
        ws = (bs.compile, bs.compileToFunctions);

    function xs(e) {
      return (gs = gs || document.createElement("div")).innerHTML = e ? '<a href="\n"/>' : '<div a="\n"/>', gs.innerHTML.indexOf("&#10;") > 0;
    }

    var $s = !!W && xs(!1),
        Cs = !!W && xs(!0),
        ks = C(function (e) {
      var t = ur(e);
      return t && t.innerHTML;
    }),
        As = Nn.prototype.$mount;
    Nn.prototype.$mount = function (e, t) {
      if ((e = e && ur(e)) === document.body || e === document.documentElement) return this;
      var n = this.$options;

      if (!n.render) {
        var r = n.template;
        if (r) {
          if ("string" == typeof r) "#" === r.charAt(0) && (r = ks(r));else {
            if (!r.nodeType) return this;
            r = r.innerHTML;
          }
        } else e && (r = function (e) {
          if (e.outerHTML) return e.outerHTML;
          var t = document.createElement("div");
          return t.appendChild(e.cloneNode(!0)), t.innerHTML;
        }(e));

        if (r) {
          0;
          var i = ws(r, {
            outputSourceRange: !1,
            shouldDecodeNewlines: $s,
            shouldDecodeNewlinesForHref: Cs,
            delimiters: n.delimiters,
            comments: n.comments
          }, this),
              o = i.render,
              a = i.staticRenderFns;
          n.render = o, n.staticRenderFns = a;
        }
      }

      return As.call(this, e, t);
    }, Nn.compile = ws;
    var Os = Nn;
    var Ss = n(9505);
    var Ts = Os.extend({
      name: "rd-copybox",
      props: {
        content: String
      },
      data: function data() {
        return {
          active: !1
        };
      },
      methods: {
        title: function title() {
          return this.content;
        },
        handleClick: function handleClick() {
          var t = this;
          return (0, e.Z)(i().mark(function e() {
            var n, r, o;
            return i().wrap(function (e) {
              for (;;) {
                switch (e.prev = e.next) {
                  case 0:
                    return n = t.$refs.content, (r = document.createRange()).selectNode(n), null == (o = window.getSelection()) || o.removeAllRanges(), e.next = 7, (0, Ss.CopyToClipboard)(t.content);

                  case 7:
                    t.active = !0, setTimeout(function () {
                      return t.active = !1;
                    }, 400), setTimeout(function () {
                      null == o || o.addRange(r);
                    }, 700);

                  case 10:
                  case "end":
                    return e.stop();
                }
              }
            }, e);
          }))();
        }
      }
    }),
        Es = Ts;
    var js = n(3379),
        Ls = n.n(js),
        Ns = n(1621),
        Ms = {
      insert: "head",
      singleton: !1
    };
    Ls()(Ns.Z, Ms);
    Ns.Z.locals;

    var Is = function (e, t, n, r, i, o, a, s) {
      var c,
          u = "function" == typeof e ? e.options : e;
      if (t && (u.render = t, u.staticRenderFns = n, u._compiled = !0), r && (u.functional = !0), o && (u._scopeId = "data-v-" + o), a ? (c = function c(e) {
        (e = e || this.$vnode && this.$vnode.ssrContext || this.parent && this.parent.$vnode && this.parent.$vnode.ssrContext) || "undefined" == typeof __VUE_SSR_CONTEXT__ || (e = __VUE_SSR_CONTEXT__), i && i.call(this, e), e && e._registeredComponents && e._registeredComponents.add(a);
      }, u._ssrRegister = c) : i && (c = s ? function () {
        i.call(this, (u.functional ? this.parent : this).$root.$options.shadowRoot);
      } : i), c) if (u.functional) {
        u._injectStyles = c;
        var l = u.render;

        u.render = function (e, t) {
          return c.call(t), l(e, t);
        };
      } else {
        var f = u.beforeCreate;
        u.beforeCreate = f ? [].concat(f, c) : [c];
      }
      return {
        exports: e,
        options: u
      };
    }(Es, function () {
      var e = this,
          t = e.$createElement,
          n = e._self._c || t;
      return n("div", {
        staticClass: "rd-copybox",
        attrs: {
          title: e.title()
        },
        on: {
          click: e.handleClick
        }
      }, [n("div", {
        ref: "content",
        staticClass: "rd-copybox__content",
        "class": {
          "rd-copybox__content--active": e.active
        }
      }, [e._v("\n        " + e._s(e.content) + "\n    ")]), e._v(" "), n("div", {
        staticClass: "rd-copybox__spacer",
        "class": {
          "rd-copybox__content--active": e.active
        }
      }), e._v(" "), e._m(0), e._v(" "), n("span", {
        staticClass: "rd-copybox__success",
        "class": {
          "rd-copybox__success--active": e.active
        }
      }, [e._v("Copied to clipboard!")])]);
    }, [function () {
      var e = this.$createElement,
          t = this._self._c || e;
      return t("div", {
        staticClass: "rd-copybox__icon"
      }, [t("i", {
        staticClass: "fas fa-clipboard"
      })]);
    }], !1, null, "bfe70aba", null);

    var Ds = Is.exports;
  })(), module.exports.rundeckUiTrellis = r;
})();

/***/ }),

/***/ "./node_modules/vue/dist/vue.runtime.esm.js":
/*!**************************************************!*\
  !*** ./node_modules/vue/dist/vue.runtime.esm.js ***!
  \**************************************************/
/***/ ((__unused_webpack_module, __webpack_exports__, __webpack_require__) => {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export */ __webpack_require__.d(__webpack_exports__, {
/* harmony export */   "default": () => (__WEBPACK_DEFAULT_EXPORT__)
/* harmony export */ });
/*!
 * Vue.js v2.6.14
 * (c) 2014-2021 Evan You
 * Released under the MIT License.
 */
/*  */

var emptyObject = Object.freeze({});

// These helpers produce better VM code in JS engines due to their
// explicitness and function inlining.
function isUndef (v) {
  return v === undefined || v === null
}

function isDef (v) {
  return v !== undefined && v !== null
}

function isTrue (v) {
  return v === true
}

function isFalse (v) {
  return v === false
}

/**
 * Check if value is primitive.
 */
function isPrimitive (value) {
  return (
    typeof value === 'string' ||
    typeof value === 'number' ||
    // $flow-disable-line
    typeof value === 'symbol' ||
    typeof value === 'boolean'
  )
}

/**
 * Quick object check - this is primarily used to tell
 * Objects from primitive values when we know the value
 * is a JSON-compliant type.
 */
function isObject (obj) {
  return obj !== null && typeof obj === 'object'
}

/**
 * Get the raw type string of a value, e.g., [object Object].
 */
var _toString = Object.prototype.toString;

function toRawType (value) {
  return _toString.call(value).slice(8, -1)
}

/**
 * Strict object type check. Only returns true
 * for plain JavaScript objects.
 */
function isPlainObject (obj) {
  return _toString.call(obj) === '[object Object]'
}

function isRegExp (v) {
  return _toString.call(v) === '[object RegExp]'
}

/**
 * Check if val is a valid array index.
 */
function isValidArrayIndex (val) {
  var n = parseFloat(String(val));
  return n >= 0 && Math.floor(n) === n && isFinite(val)
}

function isPromise (val) {
  return (
    isDef(val) &&
    typeof val.then === 'function' &&
    typeof val.catch === 'function'
  )
}

/**
 * Convert a value to a string that is actually rendered.
 */
function toString (val) {
  return val == null
    ? ''
    : Array.isArray(val) || (isPlainObject(val) && val.toString === _toString)
      ? JSON.stringify(val, null, 2)
      : String(val)
}

/**
 * Convert an input value to a number for persistence.
 * If the conversion fails, return original string.
 */
function toNumber (val) {
  var n = parseFloat(val);
  return isNaN(n) ? val : n
}

/**
 * Make a map and return a function for checking if a key
 * is in that map.
 */
function makeMap (
  str,
  expectsLowerCase
) {
  var map = Object.create(null);
  var list = str.split(',');
  for (var i = 0; i < list.length; i++) {
    map[list[i]] = true;
  }
  return expectsLowerCase
    ? function (val) { return map[val.toLowerCase()]; }
    : function (val) { return map[val]; }
}

/**
 * Check if a tag is a built-in tag.
 */
var isBuiltInTag = makeMap('slot,component', true);

/**
 * Check if an attribute is a reserved attribute.
 */
var isReservedAttribute = makeMap('key,ref,slot,slot-scope,is');

/**
 * Remove an item from an array.
 */
function remove (arr, item) {
  if (arr.length) {
    var index = arr.indexOf(item);
    if (index > -1) {
      return arr.splice(index, 1)
    }
  }
}

/**
 * Check whether an object has the property.
 */
var hasOwnProperty = Object.prototype.hasOwnProperty;
function hasOwn (obj, key) {
  return hasOwnProperty.call(obj, key)
}

/**
 * Create a cached version of a pure function.
 */
function cached (fn) {
  var cache = Object.create(null);
  return (function cachedFn (str) {
    var hit = cache[str];
    return hit || (cache[str] = fn(str))
  })
}

/**
 * Camelize a hyphen-delimited string.
 */
var camelizeRE = /-(\w)/g;
var camelize = cached(function (str) {
  return str.replace(camelizeRE, function (_, c) { return c ? c.toUpperCase() : ''; })
});

/**
 * Capitalize a string.
 */
var capitalize = cached(function (str) {
  return str.charAt(0).toUpperCase() + str.slice(1)
});

/**
 * Hyphenate a camelCase string.
 */
var hyphenateRE = /\B([A-Z])/g;
var hyphenate = cached(function (str) {
  return str.replace(hyphenateRE, '-$1').toLowerCase()
});

/**
 * Simple bind polyfill for environments that do not support it,
 * e.g., PhantomJS 1.x. Technically, we don't need this anymore
 * since native bind is now performant enough in most browsers.
 * But removing it would mean breaking code that was able to run in
 * PhantomJS 1.x, so this must be kept for backward compatibility.
 */

/* istanbul ignore next */
function polyfillBind (fn, ctx) {
  function boundFn (a) {
    var l = arguments.length;
    return l
      ? l > 1
        ? fn.apply(ctx, arguments)
        : fn.call(ctx, a)
      : fn.call(ctx)
  }

  boundFn._length = fn.length;
  return boundFn
}

function nativeBind (fn, ctx) {
  return fn.bind(ctx)
}

var bind = Function.prototype.bind
  ? nativeBind
  : polyfillBind;

/**
 * Convert an Array-like object to a real Array.
 */
function toArray (list, start) {
  start = start || 0;
  var i = list.length - start;
  var ret = new Array(i);
  while (i--) {
    ret[i] = list[i + start];
  }
  return ret
}

/**
 * Mix properties into target object.
 */
function extend (to, _from) {
  for (var key in _from) {
    to[key] = _from[key];
  }
  return to
}

/**
 * Merge an Array of Objects into a single Object.
 */
function toObject (arr) {
  var res = {};
  for (var i = 0; i < arr.length; i++) {
    if (arr[i]) {
      extend(res, arr[i]);
    }
  }
  return res
}

/* eslint-disable no-unused-vars */

/**
 * Perform no operation.
 * Stubbing args to make Flow happy without leaving useless transpiled code
 * with ...rest (https://flow.org/blog/2017/05/07/Strict-Function-Call-Arity/).
 */
function noop (a, b, c) {}

/**
 * Always return false.
 */
var no = function (a, b, c) { return false; };

/* eslint-enable no-unused-vars */

/**
 * Return the same value.
 */
var identity = function (_) { return _; };

/**
 * Check if two values are loosely equal - that is,
 * if they are plain objects, do they have the same shape?
 */
function looseEqual (a, b) {
  if (a === b) { return true }
  var isObjectA = isObject(a);
  var isObjectB = isObject(b);
  if (isObjectA && isObjectB) {
    try {
      var isArrayA = Array.isArray(a);
      var isArrayB = Array.isArray(b);
      if (isArrayA && isArrayB) {
        return a.length === b.length && a.every(function (e, i) {
          return looseEqual(e, b[i])
        })
      } else if (a instanceof Date && b instanceof Date) {
        return a.getTime() === b.getTime()
      } else if (!isArrayA && !isArrayB) {
        var keysA = Object.keys(a);
        var keysB = Object.keys(b);
        return keysA.length === keysB.length && keysA.every(function (key) {
          return looseEqual(a[key], b[key])
        })
      } else {
        /* istanbul ignore next */
        return false
      }
    } catch (e) {
      /* istanbul ignore next */
      return false
    }
  } else if (!isObjectA && !isObjectB) {
    return String(a) === String(b)
  } else {
    return false
  }
}

/**
 * Return the first index at which a loosely equal value can be
 * found in the array (if value is a plain object, the array must
 * contain an object of the same shape), or -1 if it is not present.
 */
function looseIndexOf (arr, val) {
  for (var i = 0; i < arr.length; i++) {
    if (looseEqual(arr[i], val)) { return i }
  }
  return -1
}

/**
 * Ensure a function is called only once.
 */
function once (fn) {
  var called = false;
  return function () {
    if (!called) {
      called = true;
      fn.apply(this, arguments);
    }
  }
}

var SSR_ATTR = 'data-server-rendered';

var ASSET_TYPES = [
  'component',
  'directive',
  'filter'
];

var LIFECYCLE_HOOKS = [
  'beforeCreate',
  'created',
  'beforeMount',
  'mounted',
  'beforeUpdate',
  'updated',
  'beforeDestroy',
  'destroyed',
  'activated',
  'deactivated',
  'errorCaptured',
  'serverPrefetch'
];

/*  */



var config = ({
  /**
   * Option merge strategies (used in core/util/options)
   */
  // $flow-disable-line
  optionMergeStrategies: Object.create(null),

  /**
   * Whether to suppress warnings.
   */
  silent: false,

  /**
   * Show production mode tip message on boot?
   */
  productionTip: "development" !== 'production',

  /**
   * Whether to enable devtools
   */
  devtools: "development" !== 'production',

  /**
   * Whether to record perf
   */
  performance: false,

  /**
   * Error handler for watcher errors
   */
  errorHandler: null,

  /**
   * Warn handler for watcher warns
   */
  warnHandler: null,

  /**
   * Ignore certain custom elements
   */
  ignoredElements: [],

  /**
   * Custom user key aliases for v-on
   */
  // $flow-disable-line
  keyCodes: Object.create(null),

  /**
   * Check if a tag is reserved so that it cannot be registered as a
   * component. This is platform-dependent and may be overwritten.
   */
  isReservedTag: no,

  /**
   * Check if an attribute is reserved so that it cannot be used as a component
   * prop. This is platform-dependent and may be overwritten.
   */
  isReservedAttr: no,

  /**
   * Check if a tag is an unknown element.
   * Platform-dependent.
   */
  isUnknownElement: no,

  /**
   * Get the namespace of an element
   */
  getTagNamespace: noop,

  /**
   * Parse the real tag name for the specific platform.
   */
  parsePlatformTagName: identity,

  /**
   * Check if an attribute must be bound using property, e.g. value
   * Platform-dependent.
   */
  mustUseProp: no,

  /**
   * Perform updates asynchronously. Intended to be used by Vue Test Utils
   * This will significantly reduce performance if set to false.
   */
  async: true,

  /**
   * Exposed for legacy reasons
   */
  _lifecycleHooks: LIFECYCLE_HOOKS
});

/*  */

/**
 * unicode letters used for parsing html tags, component names and property paths.
 * using https://www.w3.org/TR/html53/semantics-scripting.html#potentialcustomelementname
 * skipping \u10000-\uEFFFF due to it freezing up PhantomJS
 */
var unicodeRegExp = /a-zA-Z\u00B7\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u037D\u037F-\u1FFF\u200C-\u200D\u203F-\u2040\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD/;

/**
 * Check if a string starts with $ or _
 */
function isReserved (str) {
  var c = (str + '').charCodeAt(0);
  return c === 0x24 || c === 0x5F
}

/**
 * Define a property.
 */
function def (obj, key, val, enumerable) {
  Object.defineProperty(obj, key, {
    value: val,
    enumerable: !!enumerable,
    writable: true,
    configurable: true
  });
}

/**
 * Parse simple path.
 */
var bailRE = new RegExp(("[^" + (unicodeRegExp.source) + ".$_\\d]"));
function parsePath (path) {
  if (bailRE.test(path)) {
    return
  }
  var segments = path.split('.');
  return function (obj) {
    for (var i = 0; i < segments.length; i++) {
      if (!obj) { return }
      obj = obj[segments[i]];
    }
    return obj
  }
}

/*  */

// can we use __proto__?
var hasProto = '__proto__' in {};

// Browser environment sniffing
var inBrowser = typeof window !== 'undefined';
var inWeex = typeof WXEnvironment !== 'undefined' && !!WXEnvironment.platform;
var weexPlatform = inWeex && WXEnvironment.platform.toLowerCase();
var UA = inBrowser && window.navigator.userAgent.toLowerCase();
var isIE = UA && /msie|trident/.test(UA);
var isIE9 = UA && UA.indexOf('msie 9.0') > 0;
var isEdge = UA && UA.indexOf('edge/') > 0;
var isAndroid = (UA && UA.indexOf('android') > 0) || (weexPlatform === 'android');
var isIOS = (UA && /iphone|ipad|ipod|ios/.test(UA)) || (weexPlatform === 'ios');
var isChrome = UA && /chrome\/\d+/.test(UA) && !isEdge;
var isPhantomJS = UA && /phantomjs/.test(UA);
var isFF = UA && UA.match(/firefox\/(\d+)/);

// Firefox has a "watch" function on Object.prototype...
var nativeWatch = ({}).watch;

var supportsPassive = false;
if (inBrowser) {
  try {
    var opts = {};
    Object.defineProperty(opts, 'passive', ({
      get: function get () {
        /* istanbul ignore next */
        supportsPassive = true;
      }
    })); // https://github.com/facebook/flow/issues/285
    window.addEventListener('test-passive', null, opts);
  } catch (e) {}
}

// this needs to be lazy-evaled because vue may be required before
// vue-server-renderer can set VUE_ENV
var _isServer;
var isServerRendering = function () {
  if (_isServer === undefined) {
    /* istanbul ignore if */
    if (!inBrowser && !inWeex && typeof __webpack_require__.g !== 'undefined') {
      // detect presence of vue-server-renderer and avoid
      // Webpack shimming the process
      _isServer = __webpack_require__.g['process'] && __webpack_require__.g['process'].env.VUE_ENV === 'server';
    } else {
      _isServer = false;
    }
  }
  return _isServer
};

// detect devtools
var devtools = inBrowser && window.__VUE_DEVTOOLS_GLOBAL_HOOK__;

/* istanbul ignore next */
function isNative (Ctor) {
  return typeof Ctor === 'function' && /native code/.test(Ctor.toString())
}

var hasSymbol =
  typeof Symbol !== 'undefined' && isNative(Symbol) &&
  typeof Reflect !== 'undefined' && isNative(Reflect.ownKeys);

var _Set;
/* istanbul ignore if */ // $flow-disable-line
if (typeof Set !== 'undefined' && isNative(Set)) {
  // use native Set when available.
  _Set = Set;
} else {
  // a non-standard Set polyfill that only works with primitive keys.
  _Set = /*@__PURE__*/(function () {
    function Set () {
      this.set = Object.create(null);
    }
    Set.prototype.has = function has (key) {
      return this.set[key] === true
    };
    Set.prototype.add = function add (key) {
      this.set[key] = true;
    };
    Set.prototype.clear = function clear () {
      this.set = Object.create(null);
    };

    return Set;
  }());
}

/*  */

var warn = noop;
var tip = noop;
var generateComponentTrace = (noop); // work around flow check
var formatComponentName = (noop);

if (true) {
  var hasConsole = typeof console !== 'undefined';
  var classifyRE = /(?:^|[-_])(\w)/g;
  var classify = function (str) { return str
    .replace(classifyRE, function (c) { return c.toUpperCase(); })
    .replace(/[-_]/g, ''); };

  warn = function (msg, vm) {
    var trace = vm ? generateComponentTrace(vm) : '';

    if (config.warnHandler) {
      config.warnHandler.call(null, msg, vm, trace);
    } else if (hasConsole && (!config.silent)) {
      console.error(("[Vue warn]: " + msg + trace));
    }
  };

  tip = function (msg, vm) {
    if (hasConsole && (!config.silent)) {
      console.warn("[Vue tip]: " + msg + (
        vm ? generateComponentTrace(vm) : ''
      ));
    }
  };

  formatComponentName = function (vm, includeFile) {
    if (vm.$root === vm) {
      return '<Root>'
    }
    var options = typeof vm === 'function' && vm.cid != null
      ? vm.options
      : vm._isVue
        ? vm.$options || vm.constructor.options
        : vm;
    var name = options.name || options._componentTag;
    var file = options.__file;
    if (!name && file) {
      var match = file.match(/([^/\\]+)\.vue$/);
      name = match && match[1];
    }

    return (
      (name ? ("<" + (classify(name)) + ">") : "<Anonymous>") +
      (file && includeFile !== false ? (" at " + file) : '')
    )
  };

  var repeat = function (str, n) {
    var res = '';
    while (n) {
      if (n % 2 === 1) { res += str; }
      if (n > 1) { str += str; }
      n >>= 1;
    }
    return res
  };

  generateComponentTrace = function (vm) {
    if (vm._isVue && vm.$parent) {
      var tree = [];
      var currentRecursiveSequence = 0;
      while (vm) {
        if (tree.length > 0) {
          var last = tree[tree.length - 1];
          if (last.constructor === vm.constructor) {
            currentRecursiveSequence++;
            vm = vm.$parent;
            continue
          } else if (currentRecursiveSequence > 0) {
            tree[tree.length - 1] = [last, currentRecursiveSequence];
            currentRecursiveSequence = 0;
          }
        }
        tree.push(vm);
        vm = vm.$parent;
      }
      return '\n\nfound in\n\n' + tree
        .map(function (vm, i) { return ("" + (i === 0 ? '---> ' : repeat(' ', 5 + i * 2)) + (Array.isArray(vm)
            ? ((formatComponentName(vm[0])) + "... (" + (vm[1]) + " recursive calls)")
            : formatComponentName(vm))); })
        .join('\n')
    } else {
      return ("\n\n(found in " + (formatComponentName(vm)) + ")")
    }
  };
}

/*  */

var uid = 0;

/**
 * A dep is an observable that can have multiple
 * directives subscribing to it.
 */
var Dep = function Dep () {
  this.id = uid++;
  this.subs = [];
};

Dep.prototype.addSub = function addSub (sub) {
  this.subs.push(sub);
};

Dep.prototype.removeSub = function removeSub (sub) {
  remove(this.subs, sub);
};

Dep.prototype.depend = function depend () {
  if (Dep.target) {
    Dep.target.addDep(this);
  }
};

Dep.prototype.notify = function notify () {
  // stabilize the subscriber list first
  var subs = this.subs.slice();
  if ( true && !config.async) {
    // subs aren't sorted in scheduler if not running async
    // we need to sort them now to make sure they fire in correct
    // order
    subs.sort(function (a, b) { return a.id - b.id; });
  }
  for (var i = 0, l = subs.length; i < l; i++) {
    subs[i].update();
  }
};

// The current target watcher being evaluated.
// This is globally unique because only one watcher
// can be evaluated at a time.
Dep.target = null;
var targetStack = [];

function pushTarget (target) {
  targetStack.push(target);
  Dep.target = target;
}

function popTarget () {
  targetStack.pop();
  Dep.target = targetStack[targetStack.length - 1];
}

/*  */

var VNode = function VNode (
  tag,
  data,
  children,
  text,
  elm,
  context,
  componentOptions,
  asyncFactory
) {
  this.tag = tag;
  this.data = data;
  this.children = children;
  this.text = text;
  this.elm = elm;
  this.ns = undefined;
  this.context = context;
  this.fnContext = undefined;
  this.fnOptions = undefined;
  this.fnScopeId = undefined;
  this.key = data && data.key;
  this.componentOptions = componentOptions;
  this.componentInstance = undefined;
  this.parent = undefined;
  this.raw = false;
  this.isStatic = false;
  this.isRootInsert = true;
  this.isComment = false;
  this.isCloned = false;
  this.isOnce = false;
  this.asyncFactory = asyncFactory;
  this.asyncMeta = undefined;
  this.isAsyncPlaceholder = false;
};

var prototypeAccessors = { child: { configurable: true } };

// DEPRECATED: alias for componentInstance for backwards compat.
/* istanbul ignore next */
prototypeAccessors.child.get = function () {
  return this.componentInstance
};

Object.defineProperties( VNode.prototype, prototypeAccessors );

var createEmptyVNode = function (text) {
  if ( text === void 0 ) text = '';

  var node = new VNode();
  node.text = text;
  node.isComment = true;
  return node
};

function createTextVNode (val) {
  return new VNode(undefined, undefined, undefined, String(val))
}

// optimized shallow clone
// used for static nodes and slot nodes because they may be reused across
// multiple renders, cloning them avoids errors when DOM manipulations rely
// on their elm reference.
function cloneVNode (vnode) {
  var cloned = new VNode(
    vnode.tag,
    vnode.data,
    // #7975
    // clone children array to avoid mutating original in case of cloning
    // a child.
    vnode.children && vnode.children.slice(),
    vnode.text,
    vnode.elm,
    vnode.context,
    vnode.componentOptions,
    vnode.asyncFactory
  );
  cloned.ns = vnode.ns;
  cloned.isStatic = vnode.isStatic;
  cloned.key = vnode.key;
  cloned.isComment = vnode.isComment;
  cloned.fnContext = vnode.fnContext;
  cloned.fnOptions = vnode.fnOptions;
  cloned.fnScopeId = vnode.fnScopeId;
  cloned.asyncMeta = vnode.asyncMeta;
  cloned.isCloned = true;
  return cloned
}

/*
 * not type checking this file because flow doesn't play well with
 * dynamically accessing methods on Array prototype
 */

var arrayProto = Array.prototype;
var arrayMethods = Object.create(arrayProto);

var methodsToPatch = [
  'push',
  'pop',
  'shift',
  'unshift',
  'splice',
  'sort',
  'reverse'
];

/**
 * Intercept mutating methods and emit events
 */
methodsToPatch.forEach(function (method) {
  // cache original method
  var original = arrayProto[method];
  def(arrayMethods, method, function mutator () {
    var args = [], len = arguments.length;
    while ( len-- ) args[ len ] = arguments[ len ];

    var result = original.apply(this, args);
    var ob = this.__ob__;
    var inserted;
    switch (method) {
      case 'push':
      case 'unshift':
        inserted = args;
        break
      case 'splice':
        inserted = args.slice(2);
        break
    }
    if (inserted) { ob.observeArray(inserted); }
    // notify change
    ob.dep.notify();
    return result
  });
});

/*  */

var arrayKeys = Object.getOwnPropertyNames(arrayMethods);

/**
 * In some cases we may want to disable observation inside a component's
 * update computation.
 */
var shouldObserve = true;

function toggleObserving (value) {
  shouldObserve = value;
}

/**
 * Observer class that is attached to each observed
 * object. Once attached, the observer converts the target
 * object's property keys into getter/setters that
 * collect dependencies and dispatch updates.
 */
var Observer = function Observer (value) {
  this.value = value;
  this.dep = new Dep();
  this.vmCount = 0;
  def(value, '__ob__', this);
  if (Array.isArray(value)) {
    if (hasProto) {
      protoAugment(value, arrayMethods);
    } else {
      copyAugment(value, arrayMethods, arrayKeys);
    }
    this.observeArray(value);
  } else {
    this.walk(value);
  }
};

/**
 * Walk through all properties and convert them into
 * getter/setters. This method should only be called when
 * value type is Object.
 */
Observer.prototype.walk = function walk (obj) {
  var keys = Object.keys(obj);
  for (var i = 0; i < keys.length; i++) {
    defineReactive$$1(obj, keys[i]);
  }
};

/**
 * Observe a list of Array items.
 */
Observer.prototype.observeArray = function observeArray (items) {
  for (var i = 0, l = items.length; i < l; i++) {
    observe(items[i]);
  }
};

// helpers

/**
 * Augment a target Object or Array by intercepting
 * the prototype chain using __proto__
 */
function protoAugment (target, src) {
  /* eslint-disable no-proto */
  target.__proto__ = src;
  /* eslint-enable no-proto */
}

/**
 * Augment a target Object or Array by defining
 * hidden properties.
 */
/* istanbul ignore next */
function copyAugment (target, src, keys) {
  for (var i = 0, l = keys.length; i < l; i++) {
    var key = keys[i];
    def(target, key, src[key]);
  }
}

/**
 * Attempt to create an observer instance for a value,
 * returns the new observer if successfully observed,
 * or the existing observer if the value already has one.
 */
function observe (value, asRootData) {
  if (!isObject(value) || value instanceof VNode) {
    return
  }
  var ob;
  if (hasOwn(value, '__ob__') && value.__ob__ instanceof Observer) {
    ob = value.__ob__;
  } else if (
    shouldObserve &&
    !isServerRendering() &&
    (Array.isArray(value) || isPlainObject(value)) &&
    Object.isExtensible(value) &&
    !value._isVue
  ) {
    ob = new Observer(value);
  }
  if (asRootData && ob) {
    ob.vmCount++;
  }
  return ob
}

/**
 * Define a reactive property on an Object.
 */
function defineReactive$$1 (
  obj,
  key,
  val,
  customSetter,
  shallow
) {
  var dep = new Dep();

  var property = Object.getOwnPropertyDescriptor(obj, key);
  if (property && property.configurable === false) {
    return
  }

  // cater for pre-defined getter/setters
  var getter = property && property.get;
  var setter = property && property.set;
  if ((!getter || setter) && arguments.length === 2) {
    val = obj[key];
  }

  var childOb = !shallow && observe(val);
  Object.defineProperty(obj, key, {
    enumerable: true,
    configurable: true,
    get: function reactiveGetter () {
      var value = getter ? getter.call(obj) : val;
      if (Dep.target) {
        dep.depend();
        if (childOb) {
          childOb.dep.depend();
          if (Array.isArray(value)) {
            dependArray(value);
          }
        }
      }
      return value
    },
    set: function reactiveSetter (newVal) {
      var value = getter ? getter.call(obj) : val;
      /* eslint-disable no-self-compare */
      if (newVal === value || (newVal !== newVal && value !== value)) {
        return
      }
      /* eslint-enable no-self-compare */
      if ( true && customSetter) {
        customSetter();
      }
      // #7981: for accessor properties without setter
      if (getter && !setter) { return }
      if (setter) {
        setter.call(obj, newVal);
      } else {
        val = newVal;
      }
      childOb = !shallow && observe(newVal);
      dep.notify();
    }
  });
}

/**
 * Set a property on an object. Adds the new property and
 * triggers change notification if the property doesn't
 * already exist.
 */
function set (target, key, val) {
  if ( true &&
    (isUndef(target) || isPrimitive(target))
  ) {
    warn(("Cannot set reactive property on undefined, null, or primitive value: " + ((target))));
  }
  if (Array.isArray(target) && isValidArrayIndex(key)) {
    target.length = Math.max(target.length, key);
    target.splice(key, 1, val);
    return val
  }
  if (key in target && !(key in Object.prototype)) {
    target[key] = val;
    return val
  }
  var ob = (target).__ob__;
  if (target._isVue || (ob && ob.vmCount)) {
     true && warn(
      'Avoid adding reactive properties to a Vue instance or its root $data ' +
      'at runtime - declare it upfront in the data option.'
    );
    return val
  }
  if (!ob) {
    target[key] = val;
    return val
  }
  defineReactive$$1(ob.value, key, val);
  ob.dep.notify();
  return val
}

/**
 * Delete a property and trigger change if necessary.
 */
function del (target, key) {
  if ( true &&
    (isUndef(target) || isPrimitive(target))
  ) {
    warn(("Cannot delete reactive property on undefined, null, or primitive value: " + ((target))));
  }
  if (Array.isArray(target) && isValidArrayIndex(key)) {
    target.splice(key, 1);
    return
  }
  var ob = (target).__ob__;
  if (target._isVue || (ob && ob.vmCount)) {
     true && warn(
      'Avoid deleting properties on a Vue instance or its root $data ' +
      '- just set it to null.'
    );
    return
  }
  if (!hasOwn(target, key)) {
    return
  }
  delete target[key];
  if (!ob) {
    return
  }
  ob.dep.notify();
}

/**
 * Collect dependencies on array elements when the array is touched, since
 * we cannot intercept array element access like property getters.
 */
function dependArray (value) {
  for (var e = (void 0), i = 0, l = value.length; i < l; i++) {
    e = value[i];
    e && e.__ob__ && e.__ob__.dep.depend();
    if (Array.isArray(e)) {
      dependArray(e);
    }
  }
}

/*  */

/**
 * Option overwriting strategies are functions that handle
 * how to merge a parent option value and a child option
 * value into the final value.
 */
var strats = config.optionMergeStrategies;

/**
 * Options with restrictions
 */
if (true) {
  strats.el = strats.propsData = function (parent, child, vm, key) {
    if (!vm) {
      warn(
        "option \"" + key + "\" can only be used during instance " +
        'creation with the `new` keyword.'
      );
    }
    return defaultStrat(parent, child)
  };
}

/**
 * Helper that recursively merges two data objects together.
 */
function mergeData (to, from) {
  if (!from) { return to }
  var key, toVal, fromVal;

  var keys = hasSymbol
    ? Reflect.ownKeys(from)
    : Object.keys(from);

  for (var i = 0; i < keys.length; i++) {
    key = keys[i];
    // in case the object is already observed...
    if (key === '__ob__') { continue }
    toVal = to[key];
    fromVal = from[key];
    if (!hasOwn(to, key)) {
      set(to, key, fromVal);
    } else if (
      toVal !== fromVal &&
      isPlainObject(toVal) &&
      isPlainObject(fromVal)
    ) {
      mergeData(toVal, fromVal);
    }
  }
  return to
}

/**
 * Data
 */
function mergeDataOrFn (
  parentVal,
  childVal,
  vm
) {
  if (!vm) {
    // in a Vue.extend merge, both should be functions
    if (!childVal) {
      return parentVal
    }
    if (!parentVal) {
      return childVal
    }
    // when parentVal & childVal are both present,
    // we need to return a function that returns the
    // merged result of both functions... no need to
    // check if parentVal is a function here because
    // it has to be a function to pass previous merges.
    return function mergedDataFn () {
      return mergeData(
        typeof childVal === 'function' ? childVal.call(this, this) : childVal,
        typeof parentVal === 'function' ? parentVal.call(this, this) : parentVal
      )
    }
  } else {
    return function mergedInstanceDataFn () {
      // instance merge
      var instanceData = typeof childVal === 'function'
        ? childVal.call(vm, vm)
        : childVal;
      var defaultData = typeof parentVal === 'function'
        ? parentVal.call(vm, vm)
        : parentVal;
      if (instanceData) {
        return mergeData(instanceData, defaultData)
      } else {
        return defaultData
      }
    }
  }
}

strats.data = function (
  parentVal,
  childVal,
  vm
) {
  if (!vm) {
    if (childVal && typeof childVal !== 'function') {
       true && warn(
        'The "data" option should be a function ' +
        'that returns a per-instance value in component ' +
        'definitions.',
        vm
      );

      return parentVal
    }
    return mergeDataOrFn(parentVal, childVal)
  }

  return mergeDataOrFn(parentVal, childVal, vm)
};

/**
 * Hooks and props are merged as arrays.
 */
function mergeHook (
  parentVal,
  childVal
) {
  var res = childVal
    ? parentVal
      ? parentVal.concat(childVal)
      : Array.isArray(childVal)
        ? childVal
        : [childVal]
    : parentVal;
  return res
    ? dedupeHooks(res)
    : res
}

function dedupeHooks (hooks) {
  var res = [];
  for (var i = 0; i < hooks.length; i++) {
    if (res.indexOf(hooks[i]) === -1) {
      res.push(hooks[i]);
    }
  }
  return res
}

LIFECYCLE_HOOKS.forEach(function (hook) {
  strats[hook] = mergeHook;
});

/**
 * Assets
 *
 * When a vm is present (instance creation), we need to do
 * a three-way merge between constructor options, instance
 * options and parent options.
 */
function mergeAssets (
  parentVal,
  childVal,
  vm,
  key
) {
  var res = Object.create(parentVal || null);
  if (childVal) {
     true && assertObjectType(key, childVal, vm);
    return extend(res, childVal)
  } else {
    return res
  }
}

ASSET_TYPES.forEach(function (type) {
  strats[type + 's'] = mergeAssets;
});

/**
 * Watchers.
 *
 * Watchers hashes should not overwrite one
 * another, so we merge them as arrays.
 */
strats.watch = function (
  parentVal,
  childVal,
  vm,
  key
) {
  // work around Firefox's Object.prototype.watch...
  if (parentVal === nativeWatch) { parentVal = undefined; }
  if (childVal === nativeWatch) { childVal = undefined; }
  /* istanbul ignore if */
  if (!childVal) { return Object.create(parentVal || null) }
  if (true) {
    assertObjectType(key, childVal, vm);
  }
  if (!parentVal) { return childVal }
  var ret = {};
  extend(ret, parentVal);
  for (var key$1 in childVal) {
    var parent = ret[key$1];
    var child = childVal[key$1];
    if (parent && !Array.isArray(parent)) {
      parent = [parent];
    }
    ret[key$1] = parent
      ? parent.concat(child)
      : Array.isArray(child) ? child : [child];
  }
  return ret
};

/**
 * Other object hashes.
 */
strats.props =
strats.methods =
strats.inject =
strats.computed = function (
  parentVal,
  childVal,
  vm,
  key
) {
  if (childVal && "development" !== 'production') {
    assertObjectType(key, childVal, vm);
  }
  if (!parentVal) { return childVal }
  var ret = Object.create(null);
  extend(ret, parentVal);
  if (childVal) { extend(ret, childVal); }
  return ret
};
strats.provide = mergeDataOrFn;

/**
 * Default strategy.
 */
var defaultStrat = function (parentVal, childVal) {
  return childVal === undefined
    ? parentVal
    : childVal
};

/**
 * Validate component names
 */
function checkComponents (options) {
  for (var key in options.components) {
    validateComponentName(key);
  }
}

function validateComponentName (name) {
  if (!new RegExp(("^[a-zA-Z][\\-\\.0-9_" + (unicodeRegExp.source) + "]*$")).test(name)) {
    warn(
      'Invalid component name: "' + name + '". Component names ' +
      'should conform to valid custom element name in html5 specification.'
    );
  }
  if (isBuiltInTag(name) || config.isReservedTag(name)) {
    warn(
      'Do not use built-in or reserved HTML elements as component ' +
      'id: ' + name
    );
  }
}

/**
 * Ensure all props option syntax are normalized into the
 * Object-based format.
 */
function normalizeProps (options, vm) {
  var props = options.props;
  if (!props) { return }
  var res = {};
  var i, val, name;
  if (Array.isArray(props)) {
    i = props.length;
    while (i--) {
      val = props[i];
      if (typeof val === 'string') {
        name = camelize(val);
        res[name] = { type: null };
      } else if (true) {
        warn('props must be strings when using array syntax.');
      }
    }
  } else if (isPlainObject(props)) {
    for (var key in props) {
      val = props[key];
      name = camelize(key);
      res[name] = isPlainObject(val)
        ? val
        : { type: val };
    }
  } else if (true) {
    warn(
      "Invalid value for option \"props\": expected an Array or an Object, " +
      "but got " + (toRawType(props)) + ".",
      vm
    );
  }
  options.props = res;
}

/**
 * Normalize all injections into Object-based format
 */
function normalizeInject (options, vm) {
  var inject = options.inject;
  if (!inject) { return }
  var normalized = options.inject = {};
  if (Array.isArray(inject)) {
    for (var i = 0; i < inject.length; i++) {
      normalized[inject[i]] = { from: inject[i] };
    }
  } else if (isPlainObject(inject)) {
    for (var key in inject) {
      var val = inject[key];
      normalized[key] = isPlainObject(val)
        ? extend({ from: key }, val)
        : { from: val };
    }
  } else if (true) {
    warn(
      "Invalid value for option \"inject\": expected an Array or an Object, " +
      "but got " + (toRawType(inject)) + ".",
      vm
    );
  }
}

/**
 * Normalize raw function directives into object format.
 */
function normalizeDirectives (options) {
  var dirs = options.directives;
  if (dirs) {
    for (var key in dirs) {
      var def$$1 = dirs[key];
      if (typeof def$$1 === 'function') {
        dirs[key] = { bind: def$$1, update: def$$1 };
      }
    }
  }
}

function assertObjectType (name, value, vm) {
  if (!isPlainObject(value)) {
    warn(
      "Invalid value for option \"" + name + "\": expected an Object, " +
      "but got " + (toRawType(value)) + ".",
      vm
    );
  }
}

/**
 * Merge two option objects into a new one.
 * Core utility used in both instantiation and inheritance.
 */
function mergeOptions (
  parent,
  child,
  vm
) {
  if (true) {
    checkComponents(child);
  }

  if (typeof child === 'function') {
    child = child.options;
  }

  normalizeProps(child, vm);
  normalizeInject(child, vm);
  normalizeDirectives(child);

  // Apply extends and mixins on the child options,
  // but only if it is a raw options object that isn't
  // the result of another mergeOptions call.
  // Only merged options has the _base property.
  if (!child._base) {
    if (child.extends) {
      parent = mergeOptions(parent, child.extends, vm);
    }
    if (child.mixins) {
      for (var i = 0, l = child.mixins.length; i < l; i++) {
        parent = mergeOptions(parent, child.mixins[i], vm);
      }
    }
  }

  var options = {};
  var key;
  for (key in parent) {
    mergeField(key);
  }
  for (key in child) {
    if (!hasOwn(parent, key)) {
      mergeField(key);
    }
  }
  function mergeField (key) {
    var strat = strats[key] || defaultStrat;
    options[key] = strat(parent[key], child[key], vm, key);
  }
  return options
}

/**
 * Resolve an asset.
 * This function is used because child instances need access
 * to assets defined in its ancestor chain.
 */
function resolveAsset (
  options,
  type,
  id,
  warnMissing
) {
  /* istanbul ignore if */
  if (typeof id !== 'string') {
    return
  }
  var assets = options[type];
  // check local registration variations first
  if (hasOwn(assets, id)) { return assets[id] }
  var camelizedId = camelize(id);
  if (hasOwn(assets, camelizedId)) { return assets[camelizedId] }
  var PascalCaseId = capitalize(camelizedId);
  if (hasOwn(assets, PascalCaseId)) { return assets[PascalCaseId] }
  // fallback to prototype chain
  var res = assets[id] || assets[camelizedId] || assets[PascalCaseId];
  if ( true && warnMissing && !res) {
    warn(
      'Failed to resolve ' + type.slice(0, -1) + ': ' + id,
      options
    );
  }
  return res
}

/*  */



function validateProp (
  key,
  propOptions,
  propsData,
  vm
) {
  var prop = propOptions[key];
  var absent = !hasOwn(propsData, key);
  var value = propsData[key];
  // boolean casting
  var booleanIndex = getTypeIndex(Boolean, prop.type);
  if (booleanIndex > -1) {
    if (absent && !hasOwn(prop, 'default')) {
      value = false;
    } else if (value === '' || value === hyphenate(key)) {
      // only cast empty string / same name to boolean if
      // boolean has higher priority
      var stringIndex = getTypeIndex(String, prop.type);
      if (stringIndex < 0 || booleanIndex < stringIndex) {
        value = true;
      }
    }
  }
  // check default value
  if (value === undefined) {
    value = getPropDefaultValue(vm, prop, key);
    // since the default value is a fresh copy,
    // make sure to observe it.
    var prevShouldObserve = shouldObserve;
    toggleObserving(true);
    observe(value);
    toggleObserving(prevShouldObserve);
  }
  if (
    true
  ) {
    assertProp(prop, key, value, vm, absent);
  }
  return value
}

/**
 * Get the default value of a prop.
 */
function getPropDefaultValue (vm, prop, key) {
  // no default, return undefined
  if (!hasOwn(prop, 'default')) {
    return undefined
  }
  var def = prop.default;
  // warn against non-factory defaults for Object & Array
  if ( true && isObject(def)) {
    warn(
      'Invalid default value for prop "' + key + '": ' +
      'Props with type Object/Array must use a factory function ' +
      'to return the default value.',
      vm
    );
  }
  // the raw prop value was also undefined from previous render,
  // return previous default value to avoid unnecessary watcher trigger
  if (vm && vm.$options.propsData &&
    vm.$options.propsData[key] === undefined &&
    vm._props[key] !== undefined
  ) {
    return vm._props[key]
  }
  // call factory function for non-Function types
  // a value is Function if its prototype is function even across different execution context
  return typeof def === 'function' && getType(prop.type) !== 'Function'
    ? def.call(vm)
    : def
}

/**
 * Assert whether a prop is valid.
 */
function assertProp (
  prop,
  name,
  value,
  vm,
  absent
) {
  if (prop.required && absent) {
    warn(
      'Missing required prop: "' + name + '"',
      vm
    );
    return
  }
  if (value == null && !prop.required) {
    return
  }
  var type = prop.type;
  var valid = !type || type === true;
  var expectedTypes = [];
  if (type) {
    if (!Array.isArray(type)) {
      type = [type];
    }
    for (var i = 0; i < type.length && !valid; i++) {
      var assertedType = assertType(value, type[i], vm);
      expectedTypes.push(assertedType.expectedType || '');
      valid = assertedType.valid;
    }
  }

  var haveExpectedTypes = expectedTypes.some(function (t) { return t; });
  if (!valid && haveExpectedTypes) {
    warn(
      getInvalidTypeMessage(name, value, expectedTypes),
      vm
    );
    return
  }
  var validator = prop.validator;
  if (validator) {
    if (!validator(value)) {
      warn(
        'Invalid prop: custom validator check failed for prop "' + name + '".',
        vm
      );
    }
  }
}

var simpleCheckRE = /^(String|Number|Boolean|Function|Symbol|BigInt)$/;

function assertType (value, type, vm) {
  var valid;
  var expectedType = getType(type);
  if (simpleCheckRE.test(expectedType)) {
    var t = typeof value;
    valid = t === expectedType.toLowerCase();
    // for primitive wrapper objects
    if (!valid && t === 'object') {
      valid = value instanceof type;
    }
  } else if (expectedType === 'Object') {
    valid = isPlainObject(value);
  } else if (expectedType === 'Array') {
    valid = Array.isArray(value);
  } else {
    try {
      valid = value instanceof type;
    } catch (e) {
      warn('Invalid prop type: "' + String(type) + '" is not a constructor', vm);
      valid = false;
    }
  }
  return {
    valid: valid,
    expectedType: expectedType
  }
}

var functionTypeCheckRE = /^\s*function (\w+)/;

/**
 * Use function string name to check built-in types,
 * because a simple equality check will fail when running
 * across different vms / iframes.
 */
function getType (fn) {
  var match = fn && fn.toString().match(functionTypeCheckRE);
  return match ? match[1] : ''
}

function isSameType (a, b) {
  return getType(a) === getType(b)
}

function getTypeIndex (type, expectedTypes) {
  if (!Array.isArray(expectedTypes)) {
    return isSameType(expectedTypes, type) ? 0 : -1
  }
  for (var i = 0, len = expectedTypes.length; i < len; i++) {
    if (isSameType(expectedTypes[i], type)) {
      return i
    }
  }
  return -1
}

function getInvalidTypeMessage (name, value, expectedTypes) {
  var message = "Invalid prop: type check failed for prop \"" + name + "\"." +
    " Expected " + (expectedTypes.map(capitalize).join(', '));
  var expectedType = expectedTypes[0];
  var receivedType = toRawType(value);
  // check if we need to specify expected value
  if (
    expectedTypes.length === 1 &&
    isExplicable(expectedType) &&
    isExplicable(typeof value) &&
    !isBoolean(expectedType, receivedType)
  ) {
    message += " with value " + (styleValue(value, expectedType));
  }
  message += ", got " + receivedType + " ";
  // check if we need to specify received value
  if (isExplicable(receivedType)) {
    message += "with value " + (styleValue(value, receivedType)) + ".";
  }
  return message
}

function styleValue (value, type) {
  if (type === 'String') {
    return ("\"" + value + "\"")
  } else if (type === 'Number') {
    return ("" + (Number(value)))
  } else {
    return ("" + value)
  }
}

var EXPLICABLE_TYPES = ['string', 'number', 'boolean'];
function isExplicable (value) {
  return EXPLICABLE_TYPES.some(function (elem) { return value.toLowerCase() === elem; })
}

function isBoolean () {
  var args = [], len = arguments.length;
  while ( len-- ) args[ len ] = arguments[ len ];

  return args.some(function (elem) { return elem.toLowerCase() === 'boolean'; })
}

/*  */

function handleError (err, vm, info) {
  // Deactivate deps tracking while processing error handler to avoid possible infinite rendering.
  // See: https://github.com/vuejs/vuex/issues/1505
  pushTarget();
  try {
    if (vm) {
      var cur = vm;
      while ((cur = cur.$parent)) {
        var hooks = cur.$options.errorCaptured;
        if (hooks) {
          for (var i = 0; i < hooks.length; i++) {
            try {
              var capture = hooks[i].call(cur, err, vm, info) === false;
              if (capture) { return }
            } catch (e) {
              globalHandleError(e, cur, 'errorCaptured hook');
            }
          }
        }
      }
    }
    globalHandleError(err, vm, info);
  } finally {
    popTarget();
  }
}

function invokeWithErrorHandling (
  handler,
  context,
  args,
  vm,
  info
) {
  var res;
  try {
    res = args ? handler.apply(context, args) : handler.call(context);
    if (res && !res._isVue && isPromise(res) && !res._handled) {
      res.catch(function (e) { return handleError(e, vm, info + " (Promise/async)"); });
      // issue #9511
      // avoid catch triggering multiple times when nested calls
      res._handled = true;
    }
  } catch (e) {
    handleError(e, vm, info);
  }
  return res
}

function globalHandleError (err, vm, info) {
  if (config.errorHandler) {
    try {
      return config.errorHandler.call(null, err, vm, info)
    } catch (e) {
      // if the user intentionally throws the original error in the handler,
      // do not log it twice
      if (e !== err) {
        logError(e, null, 'config.errorHandler');
      }
    }
  }
  logError(err, vm, info);
}

function logError (err, vm, info) {
  if (true) {
    warn(("Error in " + info + ": \"" + (err.toString()) + "\""), vm);
  }
  /* istanbul ignore else */
  if ((inBrowser || inWeex) && typeof console !== 'undefined') {
    console.error(err);
  } else {
    throw err
  }
}

/*  */

var isUsingMicroTask = false;

var callbacks = [];
var pending = false;

function flushCallbacks () {
  pending = false;
  var copies = callbacks.slice(0);
  callbacks.length = 0;
  for (var i = 0; i < copies.length; i++) {
    copies[i]();
  }
}

// Here we have async deferring wrappers using microtasks.
// In 2.5 we used (macro) tasks (in combination with microtasks).
// However, it has subtle problems when state is changed right before repaint
// (e.g. #6813, out-in transitions).
// Also, using (macro) tasks in event handler would cause some weird behaviors
// that cannot be circumvented (e.g. #7109, #7153, #7546, #7834, #8109).
// So we now use microtasks everywhere, again.
// A major drawback of this tradeoff is that there are some scenarios
// where microtasks have too high a priority and fire in between supposedly
// sequential events (e.g. #4521, #6690, which have workarounds)
// or even between bubbling of the same event (#6566).
var timerFunc;

// The nextTick behavior leverages the microtask queue, which can be accessed
// via either native Promise.then or MutationObserver.
// MutationObserver has wider support, however it is seriously bugged in
// UIWebView in iOS >= 9.3.3 when triggered in touch event handlers. It
// completely stops working after triggering a few times... so, if native
// Promise is available, we will use it:
/* istanbul ignore next, $flow-disable-line */
if (typeof Promise !== 'undefined' && isNative(Promise)) {
  var p = Promise.resolve();
  timerFunc = function () {
    p.then(flushCallbacks);
    // In problematic UIWebViews, Promise.then doesn't completely break, but
    // it can get stuck in a weird state where callbacks are pushed into the
    // microtask queue but the queue isn't being flushed, until the browser
    // needs to do some other work, e.g. handle a timer. Therefore we can
    // "force" the microtask queue to be flushed by adding an empty timer.
    if (isIOS) { setTimeout(noop); }
  };
  isUsingMicroTask = true;
} else if (!isIE && typeof MutationObserver !== 'undefined' && (
  isNative(MutationObserver) ||
  // PhantomJS and iOS 7.x
  MutationObserver.toString() === '[object MutationObserverConstructor]'
)) {
  // Use MutationObserver where native Promise is not available,
  // e.g. PhantomJS, iOS7, Android 4.4
  // (#6466 MutationObserver is unreliable in IE11)
  var counter = 1;
  var observer = new MutationObserver(flushCallbacks);
  var textNode = document.createTextNode(String(counter));
  observer.observe(textNode, {
    characterData: true
  });
  timerFunc = function () {
    counter = (counter + 1) % 2;
    textNode.data = String(counter);
  };
  isUsingMicroTask = true;
} else if (typeof setImmediate !== 'undefined' && isNative(setImmediate)) {
  // Fallback to setImmediate.
  // Technically it leverages the (macro) task queue,
  // but it is still a better choice than setTimeout.
  timerFunc = function () {
    setImmediate(flushCallbacks);
  };
} else {
  // Fallback to setTimeout.
  timerFunc = function () {
    setTimeout(flushCallbacks, 0);
  };
}

function nextTick (cb, ctx) {
  var _resolve;
  callbacks.push(function () {
    if (cb) {
      try {
        cb.call(ctx);
      } catch (e) {
        handleError(e, ctx, 'nextTick');
      }
    } else if (_resolve) {
      _resolve(ctx);
    }
  });
  if (!pending) {
    pending = true;
    timerFunc();
  }
  // $flow-disable-line
  if (!cb && typeof Promise !== 'undefined') {
    return new Promise(function (resolve) {
      _resolve = resolve;
    })
  }
}

/*  */

/* not type checking this file because flow doesn't play well with Proxy */

var initProxy;

if (true) {
  var allowedGlobals = makeMap(
    'Infinity,undefined,NaN,isFinite,isNaN,' +
    'parseFloat,parseInt,decodeURI,decodeURIComponent,encodeURI,encodeURIComponent,' +
    'Math,Number,Date,Array,Object,Boolean,String,RegExp,Map,Set,JSON,Intl,BigInt,' +
    'require' // for Webpack/Browserify
  );

  var warnNonPresent = function (target, key) {
    warn(
      "Property or method \"" + key + "\" is not defined on the instance but " +
      'referenced during render. Make sure that this property is reactive, ' +
      'either in the data option, or for class-based components, by ' +
      'initializing the property. ' +
      'See: https://vuejs.org/v2/guide/reactivity.html#Declaring-Reactive-Properties.',
      target
    );
  };

  var warnReservedPrefix = function (target, key) {
    warn(
      "Property \"" + key + "\" must be accessed with \"$data." + key + "\" because " +
      'properties starting with "$" or "_" are not proxied in the Vue instance to ' +
      'prevent conflicts with Vue internals. ' +
      'See: https://vuejs.org/v2/api/#data',
      target
    );
  };

  var hasProxy =
    typeof Proxy !== 'undefined' && isNative(Proxy);

  if (hasProxy) {
    var isBuiltInModifier = makeMap('stop,prevent,self,ctrl,shift,alt,meta,exact');
    config.keyCodes = new Proxy(config.keyCodes, {
      set: function set (target, key, value) {
        if (isBuiltInModifier(key)) {
          warn(("Avoid overwriting built-in modifier in config.keyCodes: ." + key));
          return false
        } else {
          target[key] = value;
          return true
        }
      }
    });
  }

  var hasHandler = {
    has: function has (target, key) {
      var has = key in target;
      var isAllowed = allowedGlobals(key) ||
        (typeof key === 'string' && key.charAt(0) === '_' && !(key in target.$data));
      if (!has && !isAllowed) {
        if (key in target.$data) { warnReservedPrefix(target, key); }
        else { warnNonPresent(target, key); }
      }
      return has || !isAllowed
    }
  };

  var getHandler = {
    get: function get (target, key) {
      if (typeof key === 'string' && !(key in target)) {
        if (key in target.$data) { warnReservedPrefix(target, key); }
        else { warnNonPresent(target, key); }
      }
      return target[key]
    }
  };

  initProxy = function initProxy (vm) {
    if (hasProxy) {
      // determine which proxy handler to use
      var options = vm.$options;
      var handlers = options.render && options.render._withStripped
        ? getHandler
        : hasHandler;
      vm._renderProxy = new Proxy(vm, handlers);
    } else {
      vm._renderProxy = vm;
    }
  };
}

/*  */

var seenObjects = new _Set();

/**
 * Recursively traverse an object to evoke all converted
 * getters, so that every nested property inside the object
 * is collected as a "deep" dependency.
 */
function traverse (val) {
  _traverse(val, seenObjects);
  seenObjects.clear();
}

function _traverse (val, seen) {
  var i, keys;
  var isA = Array.isArray(val);
  if ((!isA && !isObject(val)) || Object.isFrozen(val) || val instanceof VNode) {
    return
  }
  if (val.__ob__) {
    var depId = val.__ob__.dep.id;
    if (seen.has(depId)) {
      return
    }
    seen.add(depId);
  }
  if (isA) {
    i = val.length;
    while (i--) { _traverse(val[i], seen); }
  } else {
    keys = Object.keys(val);
    i = keys.length;
    while (i--) { _traverse(val[keys[i]], seen); }
  }
}

var mark;
var measure;

if (true) {
  var perf = inBrowser && window.performance;
  /* istanbul ignore if */
  if (
    perf &&
    perf.mark &&
    perf.measure &&
    perf.clearMarks &&
    perf.clearMeasures
  ) {
    mark = function (tag) { return perf.mark(tag); };
    measure = function (name, startTag, endTag) {
      perf.measure(name, startTag, endTag);
      perf.clearMarks(startTag);
      perf.clearMarks(endTag);
      // perf.clearMeasures(name)
    };
  }
}

/*  */

var normalizeEvent = cached(function (name) {
  var passive = name.charAt(0) === '&';
  name = passive ? name.slice(1) : name;
  var once$$1 = name.charAt(0) === '~'; // Prefixed last, checked first
  name = once$$1 ? name.slice(1) : name;
  var capture = name.charAt(0) === '!';
  name = capture ? name.slice(1) : name;
  return {
    name: name,
    once: once$$1,
    capture: capture,
    passive: passive
  }
});

function createFnInvoker (fns, vm) {
  function invoker () {
    var arguments$1 = arguments;

    var fns = invoker.fns;
    if (Array.isArray(fns)) {
      var cloned = fns.slice();
      for (var i = 0; i < cloned.length; i++) {
        invokeWithErrorHandling(cloned[i], null, arguments$1, vm, "v-on handler");
      }
    } else {
      // return handler return value for single handlers
      return invokeWithErrorHandling(fns, null, arguments, vm, "v-on handler")
    }
  }
  invoker.fns = fns;
  return invoker
}

function updateListeners (
  on,
  oldOn,
  add,
  remove$$1,
  createOnceHandler,
  vm
) {
  var name, def$$1, cur, old, event;
  for (name in on) {
    def$$1 = cur = on[name];
    old = oldOn[name];
    event = normalizeEvent(name);
    if (isUndef(cur)) {
       true && warn(
        "Invalid handler for event \"" + (event.name) + "\": got " + String(cur),
        vm
      );
    } else if (isUndef(old)) {
      if (isUndef(cur.fns)) {
        cur = on[name] = createFnInvoker(cur, vm);
      }
      if (isTrue(event.once)) {
        cur = on[name] = createOnceHandler(event.name, cur, event.capture);
      }
      add(event.name, cur, event.capture, event.passive, event.params);
    } else if (cur !== old) {
      old.fns = cur;
      on[name] = old;
    }
  }
  for (name in oldOn) {
    if (isUndef(on[name])) {
      event = normalizeEvent(name);
      remove$$1(event.name, oldOn[name], event.capture);
    }
  }
}

/*  */

function mergeVNodeHook (def, hookKey, hook) {
  if (def instanceof VNode) {
    def = def.data.hook || (def.data.hook = {});
  }
  var invoker;
  var oldHook = def[hookKey];

  function wrappedHook () {
    hook.apply(this, arguments);
    // important: remove merged hook to ensure it's called only once
    // and prevent memory leak
    remove(invoker.fns, wrappedHook);
  }

  if (isUndef(oldHook)) {
    // no existing hook
    invoker = createFnInvoker([wrappedHook]);
  } else {
    /* istanbul ignore if */
    if (isDef(oldHook.fns) && isTrue(oldHook.merged)) {
      // already a merged invoker
      invoker = oldHook;
      invoker.fns.push(wrappedHook);
    } else {
      // existing plain hook
      invoker = createFnInvoker([oldHook, wrappedHook]);
    }
  }

  invoker.merged = true;
  def[hookKey] = invoker;
}

/*  */

function extractPropsFromVNodeData (
  data,
  Ctor,
  tag
) {
  // we are only extracting raw values here.
  // validation and default values are handled in the child
  // component itself.
  var propOptions = Ctor.options.props;
  if (isUndef(propOptions)) {
    return
  }
  var res = {};
  var attrs = data.attrs;
  var props = data.props;
  if (isDef(attrs) || isDef(props)) {
    for (var key in propOptions) {
      var altKey = hyphenate(key);
      if (true) {
        var keyInLowerCase = key.toLowerCase();
        if (
          key !== keyInLowerCase &&
          attrs && hasOwn(attrs, keyInLowerCase)
        ) {
          tip(
            "Prop \"" + keyInLowerCase + "\" is passed to component " +
            (formatComponentName(tag || Ctor)) + ", but the declared prop name is" +
            " \"" + key + "\". " +
            "Note that HTML attributes are case-insensitive and camelCased " +
            "props need to use their kebab-case equivalents when using in-DOM " +
            "templates. You should probably use \"" + altKey + "\" instead of \"" + key + "\"."
          );
        }
      }
      checkProp(res, props, key, altKey, true) ||
      checkProp(res, attrs, key, altKey, false);
    }
  }
  return res
}

function checkProp (
  res,
  hash,
  key,
  altKey,
  preserve
) {
  if (isDef(hash)) {
    if (hasOwn(hash, key)) {
      res[key] = hash[key];
      if (!preserve) {
        delete hash[key];
      }
      return true
    } else if (hasOwn(hash, altKey)) {
      res[key] = hash[altKey];
      if (!preserve) {
        delete hash[altKey];
      }
      return true
    }
  }
  return false
}

/*  */

// The template compiler attempts to minimize the need for normalization by
// statically analyzing the template at compile time.
//
// For plain HTML markup, normalization can be completely skipped because the
// generated render function is guaranteed to return Array<VNode>. There are
// two cases where extra normalization is needed:

// 1. When the children contains components - because a functional component
// may return an Array instead of a single root. In this case, just a simple
// normalization is needed - if any child is an Array, we flatten the whole
// thing with Array.prototype.concat. It is guaranteed to be only 1-level deep
// because functional components already normalize their own children.
function simpleNormalizeChildren (children) {
  for (var i = 0; i < children.length; i++) {
    if (Array.isArray(children[i])) {
      return Array.prototype.concat.apply([], children)
    }
  }
  return children
}

// 2. When the children contains constructs that always generated nested Arrays,
// e.g. <template>, <slot>, v-for, or when the children is provided by user
// with hand-written render functions / JSX. In such cases a full normalization
// is needed to cater to all possible types of children values.
function normalizeChildren (children) {
  return isPrimitive(children)
    ? [createTextVNode(children)]
    : Array.isArray(children)
      ? normalizeArrayChildren(children)
      : undefined
}

function isTextNode (node) {
  return isDef(node) && isDef(node.text) && isFalse(node.isComment)
}

function normalizeArrayChildren (children, nestedIndex) {
  var res = [];
  var i, c, lastIndex, last;
  for (i = 0; i < children.length; i++) {
    c = children[i];
    if (isUndef(c) || typeof c === 'boolean') { continue }
    lastIndex = res.length - 1;
    last = res[lastIndex];
    //  nested
    if (Array.isArray(c)) {
      if (c.length > 0) {
        c = normalizeArrayChildren(c, ((nestedIndex || '') + "_" + i));
        // merge adjacent text nodes
        if (isTextNode(c[0]) && isTextNode(last)) {
          res[lastIndex] = createTextVNode(last.text + (c[0]).text);
          c.shift();
        }
        res.push.apply(res, c);
      }
    } else if (isPrimitive(c)) {
      if (isTextNode(last)) {
        // merge adjacent text nodes
        // this is necessary for SSR hydration because text nodes are
        // essentially merged when rendered to HTML strings
        res[lastIndex] = createTextVNode(last.text + c);
      } else if (c !== '') {
        // convert primitive to vnode
        res.push(createTextVNode(c));
      }
    } else {
      if (isTextNode(c) && isTextNode(last)) {
        // merge adjacent text nodes
        res[lastIndex] = createTextVNode(last.text + c.text);
      } else {
        // default key for nested array children (likely generated by v-for)
        if (isTrue(children._isVList) &&
          isDef(c.tag) &&
          isUndef(c.key) &&
          isDef(nestedIndex)) {
          c.key = "__vlist" + nestedIndex + "_" + i + "__";
        }
        res.push(c);
      }
    }
  }
  return res
}

/*  */

function initProvide (vm) {
  var provide = vm.$options.provide;
  if (provide) {
    vm._provided = typeof provide === 'function'
      ? provide.call(vm)
      : provide;
  }
}

function initInjections (vm) {
  var result = resolveInject(vm.$options.inject, vm);
  if (result) {
    toggleObserving(false);
    Object.keys(result).forEach(function (key) {
      /* istanbul ignore else */
      if (true) {
        defineReactive$$1(vm, key, result[key], function () {
          warn(
            "Avoid mutating an injected value directly since the changes will be " +
            "overwritten whenever the provided component re-renders. " +
            "injection being mutated: \"" + key + "\"",
            vm
          );
        });
      } else {}
    });
    toggleObserving(true);
  }
}

function resolveInject (inject, vm) {
  if (inject) {
    // inject is :any because flow is not smart enough to figure out cached
    var result = Object.create(null);
    var keys = hasSymbol
      ? Reflect.ownKeys(inject)
      : Object.keys(inject);

    for (var i = 0; i < keys.length; i++) {
      var key = keys[i];
      // #6574 in case the inject object is observed...
      if (key === '__ob__') { continue }
      var provideKey = inject[key].from;
      var source = vm;
      while (source) {
        if (source._provided && hasOwn(source._provided, provideKey)) {
          result[key] = source._provided[provideKey];
          break
        }
        source = source.$parent;
      }
      if (!source) {
        if ('default' in inject[key]) {
          var provideDefault = inject[key].default;
          result[key] = typeof provideDefault === 'function'
            ? provideDefault.call(vm)
            : provideDefault;
        } else if (true) {
          warn(("Injection \"" + key + "\" not found"), vm);
        }
      }
    }
    return result
  }
}

/*  */



/**
 * Runtime helper for resolving raw children VNodes into a slot object.
 */
function resolveSlots (
  children,
  context
) {
  if (!children || !children.length) {
    return {}
  }
  var slots = {};
  for (var i = 0, l = children.length; i < l; i++) {
    var child = children[i];
    var data = child.data;
    // remove slot attribute if the node is resolved as a Vue slot node
    if (data && data.attrs && data.attrs.slot) {
      delete data.attrs.slot;
    }
    // named slots should only be respected if the vnode was rendered in the
    // same context.
    if ((child.context === context || child.fnContext === context) &&
      data && data.slot != null
    ) {
      var name = data.slot;
      var slot = (slots[name] || (slots[name] = []));
      if (child.tag === 'template') {
        slot.push.apply(slot, child.children || []);
      } else {
        slot.push(child);
      }
    } else {
      (slots.default || (slots.default = [])).push(child);
    }
  }
  // ignore slots that contains only whitespace
  for (var name$1 in slots) {
    if (slots[name$1].every(isWhitespace)) {
      delete slots[name$1];
    }
  }
  return slots
}

function isWhitespace (node) {
  return (node.isComment && !node.asyncFactory) || node.text === ' '
}

/*  */

function isAsyncPlaceholder (node) {
  return node.isComment && node.asyncFactory
}

/*  */

function normalizeScopedSlots (
  slots,
  normalSlots,
  prevSlots
) {
  var res;
  var hasNormalSlots = Object.keys(normalSlots).length > 0;
  var isStable = slots ? !!slots.$stable : !hasNormalSlots;
  var key = slots && slots.$key;
  if (!slots) {
    res = {};
  } else if (slots._normalized) {
    // fast path 1: child component re-render only, parent did not change
    return slots._normalized
  } else if (
    isStable &&
    prevSlots &&
    prevSlots !== emptyObject &&
    key === prevSlots.$key &&
    !hasNormalSlots &&
    !prevSlots.$hasNormal
  ) {
    // fast path 2: stable scoped slots w/ no normal slots to proxy,
    // only need to normalize once
    return prevSlots
  } else {
    res = {};
    for (var key$1 in slots) {
      if (slots[key$1] && key$1[0] !== '$') {
        res[key$1] = normalizeScopedSlot(normalSlots, key$1, slots[key$1]);
      }
    }
  }
  // expose normal slots on scopedSlots
  for (var key$2 in normalSlots) {
    if (!(key$2 in res)) {
      res[key$2] = proxyNormalSlot(normalSlots, key$2);
    }
  }
  // avoriaz seems to mock a non-extensible $scopedSlots object
  // and when that is passed down this would cause an error
  if (slots && Object.isExtensible(slots)) {
    (slots)._normalized = res;
  }
  def(res, '$stable', isStable);
  def(res, '$key', key);
  def(res, '$hasNormal', hasNormalSlots);
  return res
}

function normalizeScopedSlot(normalSlots, key, fn) {
  var normalized = function () {
    var res = arguments.length ? fn.apply(null, arguments) : fn({});
    res = res && typeof res === 'object' && !Array.isArray(res)
      ? [res] // single vnode
      : normalizeChildren(res);
    var vnode = res && res[0];
    return res && (
      !vnode ||
      (res.length === 1 && vnode.isComment && !isAsyncPlaceholder(vnode)) // #9658, #10391
    ) ? undefined
      : res
  };
  // this is a slot using the new v-slot syntax without scope. although it is
  // compiled as a scoped slot, render fn users would expect it to be present
  // on this.$slots because the usage is semantically a normal slot.
  if (fn.proxy) {
    Object.defineProperty(normalSlots, key, {
      get: normalized,
      enumerable: true,
      configurable: true
    });
  }
  return normalized
}

function proxyNormalSlot(slots, key) {
  return function () { return slots[key]; }
}

/*  */

/**
 * Runtime helper for rendering v-for lists.
 */
function renderList (
  val,
  render
) {
  var ret, i, l, keys, key;
  if (Array.isArray(val) || typeof val === 'string') {
    ret = new Array(val.length);
    for (i = 0, l = val.length; i < l; i++) {
      ret[i] = render(val[i], i);
    }
  } else if (typeof val === 'number') {
    ret = new Array(val);
    for (i = 0; i < val; i++) {
      ret[i] = render(i + 1, i);
    }
  } else if (isObject(val)) {
    if (hasSymbol && val[Symbol.iterator]) {
      ret = [];
      var iterator = val[Symbol.iterator]();
      var result = iterator.next();
      while (!result.done) {
        ret.push(render(result.value, ret.length));
        result = iterator.next();
      }
    } else {
      keys = Object.keys(val);
      ret = new Array(keys.length);
      for (i = 0, l = keys.length; i < l; i++) {
        key = keys[i];
        ret[i] = render(val[key], key, i);
      }
    }
  }
  if (!isDef(ret)) {
    ret = [];
  }
  (ret)._isVList = true;
  return ret
}

/*  */

/**
 * Runtime helper for rendering <slot>
 */
function renderSlot (
  name,
  fallbackRender,
  props,
  bindObject
) {
  var scopedSlotFn = this.$scopedSlots[name];
  var nodes;
  if (scopedSlotFn) {
    // scoped slot
    props = props || {};
    if (bindObject) {
      if ( true && !isObject(bindObject)) {
        warn('slot v-bind without argument expects an Object', this);
      }
      props = extend(extend({}, bindObject), props);
    }
    nodes =
      scopedSlotFn(props) ||
      (typeof fallbackRender === 'function' ? fallbackRender() : fallbackRender);
  } else {
    nodes =
      this.$slots[name] ||
      (typeof fallbackRender === 'function' ? fallbackRender() : fallbackRender);
  }

  var target = props && props.slot;
  if (target) {
    return this.$createElement('template', { slot: target }, nodes)
  } else {
    return nodes
  }
}

/*  */

/**
 * Runtime helper for resolving filters
 */
function resolveFilter (id) {
  return resolveAsset(this.$options, 'filters', id, true) || identity
}

/*  */

function isKeyNotMatch (expect, actual) {
  if (Array.isArray(expect)) {
    return expect.indexOf(actual) === -1
  } else {
    return expect !== actual
  }
}

/**
 * Runtime helper for checking keyCodes from config.
 * exposed as Vue.prototype._k
 * passing in eventKeyName as last argument separately for backwards compat
 */
function checkKeyCodes (
  eventKeyCode,
  key,
  builtInKeyCode,
  eventKeyName,
  builtInKeyName
) {
  var mappedKeyCode = config.keyCodes[key] || builtInKeyCode;
  if (builtInKeyName && eventKeyName && !config.keyCodes[key]) {
    return isKeyNotMatch(builtInKeyName, eventKeyName)
  } else if (mappedKeyCode) {
    return isKeyNotMatch(mappedKeyCode, eventKeyCode)
  } else if (eventKeyName) {
    return hyphenate(eventKeyName) !== key
  }
  return eventKeyCode === undefined
}

/*  */

/**
 * Runtime helper for merging v-bind="object" into a VNode's data.
 */
function bindObjectProps (
  data,
  tag,
  value,
  asProp,
  isSync
) {
  if (value) {
    if (!isObject(value)) {
       true && warn(
        'v-bind without argument expects an Object or Array value',
        this
      );
    } else {
      if (Array.isArray(value)) {
        value = toObject(value);
      }
      var hash;
      var loop = function ( key ) {
        if (
          key === 'class' ||
          key === 'style' ||
          isReservedAttribute(key)
        ) {
          hash = data;
        } else {
          var type = data.attrs && data.attrs.type;
          hash = asProp || config.mustUseProp(tag, type, key)
            ? data.domProps || (data.domProps = {})
            : data.attrs || (data.attrs = {});
        }
        var camelizedKey = camelize(key);
        var hyphenatedKey = hyphenate(key);
        if (!(camelizedKey in hash) && !(hyphenatedKey in hash)) {
          hash[key] = value[key];

          if (isSync) {
            var on = data.on || (data.on = {});
            on[("update:" + key)] = function ($event) {
              value[key] = $event;
            };
          }
        }
      };

      for (var key in value) loop( key );
    }
  }
  return data
}

/*  */

/**
 * Runtime helper for rendering static trees.
 */
function renderStatic (
  index,
  isInFor
) {
  var cached = this._staticTrees || (this._staticTrees = []);
  var tree = cached[index];
  // if has already-rendered static tree and not inside v-for,
  // we can reuse the same tree.
  if (tree && !isInFor) {
    return tree
  }
  // otherwise, render a fresh tree.
  tree = cached[index] = this.$options.staticRenderFns[index].call(
    this._renderProxy,
    null,
    this // for render fns generated for functional component templates
  );
  markStatic(tree, ("__static__" + index), false);
  return tree
}

/**
 * Runtime helper for v-once.
 * Effectively it means marking the node as static with a unique key.
 */
function markOnce (
  tree,
  index,
  key
) {
  markStatic(tree, ("__once__" + index + (key ? ("_" + key) : "")), true);
  return tree
}

function markStatic (
  tree,
  key,
  isOnce
) {
  if (Array.isArray(tree)) {
    for (var i = 0; i < tree.length; i++) {
      if (tree[i] && typeof tree[i] !== 'string') {
        markStaticNode(tree[i], (key + "_" + i), isOnce);
      }
    }
  } else {
    markStaticNode(tree, key, isOnce);
  }
}

function markStaticNode (node, key, isOnce) {
  node.isStatic = true;
  node.key = key;
  node.isOnce = isOnce;
}

/*  */

function bindObjectListeners (data, value) {
  if (value) {
    if (!isPlainObject(value)) {
       true && warn(
        'v-on without argument expects an Object value',
        this
      );
    } else {
      var on = data.on = data.on ? extend({}, data.on) : {};
      for (var key in value) {
        var existing = on[key];
        var ours = value[key];
        on[key] = existing ? [].concat(existing, ours) : ours;
      }
    }
  }
  return data
}

/*  */

function resolveScopedSlots (
  fns, // see flow/vnode
  res,
  // the following are added in 2.6
  hasDynamicKeys,
  contentHashKey
) {
  res = res || { $stable: !hasDynamicKeys };
  for (var i = 0; i < fns.length; i++) {
    var slot = fns[i];
    if (Array.isArray(slot)) {
      resolveScopedSlots(slot, res, hasDynamicKeys);
    } else if (slot) {
      // marker for reverse proxying v-slot without scope on this.$slots
      if (slot.proxy) {
        slot.fn.proxy = true;
      }
      res[slot.key] = slot.fn;
    }
  }
  if (contentHashKey) {
    (res).$key = contentHashKey;
  }
  return res
}

/*  */

function bindDynamicKeys (baseObj, values) {
  for (var i = 0; i < values.length; i += 2) {
    var key = values[i];
    if (typeof key === 'string' && key) {
      baseObj[values[i]] = values[i + 1];
    } else if ( true && key !== '' && key !== null) {
      // null is a special value for explicitly removing a binding
      warn(
        ("Invalid value for dynamic directive argument (expected string or null): " + key),
        this
      );
    }
  }
  return baseObj
}

// helper to dynamically append modifier runtime markers to event names.
// ensure only append when value is already string, otherwise it will be cast
// to string and cause the type check to miss.
function prependModifier (value, symbol) {
  return typeof value === 'string' ? symbol + value : value
}

/*  */

function installRenderHelpers (target) {
  target._o = markOnce;
  target._n = toNumber;
  target._s = toString;
  target._l = renderList;
  target._t = renderSlot;
  target._q = looseEqual;
  target._i = looseIndexOf;
  target._m = renderStatic;
  target._f = resolveFilter;
  target._k = checkKeyCodes;
  target._b = bindObjectProps;
  target._v = createTextVNode;
  target._e = createEmptyVNode;
  target._u = resolveScopedSlots;
  target._g = bindObjectListeners;
  target._d = bindDynamicKeys;
  target._p = prependModifier;
}

/*  */

function FunctionalRenderContext (
  data,
  props,
  children,
  parent,
  Ctor
) {
  var this$1 = this;

  var options = Ctor.options;
  // ensure the createElement function in functional components
  // gets a unique context - this is necessary for correct named slot check
  var contextVm;
  if (hasOwn(parent, '_uid')) {
    contextVm = Object.create(parent);
    // $flow-disable-line
    contextVm._original = parent;
  } else {
    // the context vm passed in is a functional context as well.
    // in this case we want to make sure we are able to get a hold to the
    // real context instance.
    contextVm = parent;
    // $flow-disable-line
    parent = parent._original;
  }
  var isCompiled = isTrue(options._compiled);
  var needNormalization = !isCompiled;

  this.data = data;
  this.props = props;
  this.children = children;
  this.parent = parent;
  this.listeners = data.on || emptyObject;
  this.injections = resolveInject(options.inject, parent);
  this.slots = function () {
    if (!this$1.$slots) {
      normalizeScopedSlots(
        data.scopedSlots,
        this$1.$slots = resolveSlots(children, parent)
      );
    }
    return this$1.$slots
  };

  Object.defineProperty(this, 'scopedSlots', ({
    enumerable: true,
    get: function get () {
      return normalizeScopedSlots(data.scopedSlots, this.slots())
    }
  }));

  // support for compiled functional template
  if (isCompiled) {
    // exposing $options for renderStatic()
    this.$options = options;
    // pre-resolve slots for renderSlot()
    this.$slots = this.slots();
    this.$scopedSlots = normalizeScopedSlots(data.scopedSlots, this.$slots);
  }

  if (options._scopeId) {
    this._c = function (a, b, c, d) {
      var vnode = createElement(contextVm, a, b, c, d, needNormalization);
      if (vnode && !Array.isArray(vnode)) {
        vnode.fnScopeId = options._scopeId;
        vnode.fnContext = parent;
      }
      return vnode
    };
  } else {
    this._c = function (a, b, c, d) { return createElement(contextVm, a, b, c, d, needNormalization); };
  }
}

installRenderHelpers(FunctionalRenderContext.prototype);

function createFunctionalComponent (
  Ctor,
  propsData,
  data,
  contextVm,
  children
) {
  var options = Ctor.options;
  var props = {};
  var propOptions = options.props;
  if (isDef(propOptions)) {
    for (var key in propOptions) {
      props[key] = validateProp(key, propOptions, propsData || emptyObject);
    }
  } else {
    if (isDef(data.attrs)) { mergeProps(props, data.attrs); }
    if (isDef(data.props)) { mergeProps(props, data.props); }
  }

  var renderContext = new FunctionalRenderContext(
    data,
    props,
    children,
    contextVm,
    Ctor
  );

  var vnode = options.render.call(null, renderContext._c, renderContext);

  if (vnode instanceof VNode) {
    return cloneAndMarkFunctionalResult(vnode, data, renderContext.parent, options, renderContext)
  } else if (Array.isArray(vnode)) {
    var vnodes = normalizeChildren(vnode) || [];
    var res = new Array(vnodes.length);
    for (var i = 0; i < vnodes.length; i++) {
      res[i] = cloneAndMarkFunctionalResult(vnodes[i], data, renderContext.parent, options, renderContext);
    }
    return res
  }
}

function cloneAndMarkFunctionalResult (vnode, data, contextVm, options, renderContext) {
  // #7817 clone node before setting fnContext, otherwise if the node is reused
  // (e.g. it was from a cached normal slot) the fnContext causes named slots
  // that should not be matched to match.
  var clone = cloneVNode(vnode);
  clone.fnContext = contextVm;
  clone.fnOptions = options;
  if (true) {
    (clone.devtoolsMeta = clone.devtoolsMeta || {}).renderContext = renderContext;
  }
  if (data.slot) {
    (clone.data || (clone.data = {})).slot = data.slot;
  }
  return clone
}

function mergeProps (to, from) {
  for (var key in from) {
    to[camelize(key)] = from[key];
  }
}

/*  */

/*  */

/*  */

/*  */

// inline hooks to be invoked on component VNodes during patch
var componentVNodeHooks = {
  init: function init (vnode, hydrating) {
    if (
      vnode.componentInstance &&
      !vnode.componentInstance._isDestroyed &&
      vnode.data.keepAlive
    ) {
      // kept-alive components, treat as a patch
      var mountedNode = vnode; // work around flow
      componentVNodeHooks.prepatch(mountedNode, mountedNode);
    } else {
      var child = vnode.componentInstance = createComponentInstanceForVnode(
        vnode,
        activeInstance
      );
      child.$mount(hydrating ? vnode.elm : undefined, hydrating);
    }
  },

  prepatch: function prepatch (oldVnode, vnode) {
    var options = vnode.componentOptions;
    var child = vnode.componentInstance = oldVnode.componentInstance;
    updateChildComponent(
      child,
      options.propsData, // updated props
      options.listeners, // updated listeners
      vnode, // new parent vnode
      options.children // new children
    );
  },

  insert: function insert (vnode) {
    var context = vnode.context;
    var componentInstance = vnode.componentInstance;
    if (!componentInstance._isMounted) {
      componentInstance._isMounted = true;
      callHook(componentInstance, 'mounted');
    }
    if (vnode.data.keepAlive) {
      if (context._isMounted) {
        // vue-router#1212
        // During updates, a kept-alive component's child components may
        // change, so directly walking the tree here may call activated hooks
        // on incorrect children. Instead we push them into a queue which will
        // be processed after the whole patch process ended.
        queueActivatedComponent(componentInstance);
      } else {
        activateChildComponent(componentInstance, true /* direct */);
      }
    }
  },

  destroy: function destroy (vnode) {
    var componentInstance = vnode.componentInstance;
    if (!componentInstance._isDestroyed) {
      if (!vnode.data.keepAlive) {
        componentInstance.$destroy();
      } else {
        deactivateChildComponent(componentInstance, true /* direct */);
      }
    }
  }
};

var hooksToMerge = Object.keys(componentVNodeHooks);

function createComponent (
  Ctor,
  data,
  context,
  children,
  tag
) {
  if (isUndef(Ctor)) {
    return
  }

  var baseCtor = context.$options._base;

  // plain options object: turn it into a constructor
  if (isObject(Ctor)) {
    Ctor = baseCtor.extend(Ctor);
  }

  // if at this stage it's not a constructor or an async component factory,
  // reject.
  if (typeof Ctor !== 'function') {
    if (true) {
      warn(("Invalid Component definition: " + (String(Ctor))), context);
    }
    return
  }

  // async component
  var asyncFactory;
  if (isUndef(Ctor.cid)) {
    asyncFactory = Ctor;
    Ctor = resolveAsyncComponent(asyncFactory, baseCtor);
    if (Ctor === undefined) {
      // return a placeholder node for async component, which is rendered
      // as a comment node but preserves all the raw information for the node.
      // the information will be used for async server-rendering and hydration.
      return createAsyncPlaceholder(
        asyncFactory,
        data,
        context,
        children,
        tag
      )
    }
  }

  data = data || {};

  // resolve constructor options in case global mixins are applied after
  // component constructor creation
  resolveConstructorOptions(Ctor);

  // transform component v-model data into props & events
  if (isDef(data.model)) {
    transformModel(Ctor.options, data);
  }

  // extract props
  var propsData = extractPropsFromVNodeData(data, Ctor, tag);

  // functional component
  if (isTrue(Ctor.options.functional)) {
    return createFunctionalComponent(Ctor, propsData, data, context, children)
  }

  // extract listeners, since these needs to be treated as
  // child component listeners instead of DOM listeners
  var listeners = data.on;
  // replace with listeners with .native modifier
  // so it gets processed during parent component patch.
  data.on = data.nativeOn;

  if (isTrue(Ctor.options.abstract)) {
    // abstract components do not keep anything
    // other than props & listeners & slot

    // work around flow
    var slot = data.slot;
    data = {};
    if (slot) {
      data.slot = slot;
    }
  }

  // install component management hooks onto the placeholder node
  installComponentHooks(data);

  // return a placeholder vnode
  var name = Ctor.options.name || tag;
  var vnode = new VNode(
    ("vue-component-" + (Ctor.cid) + (name ? ("-" + name) : '')),
    data, undefined, undefined, undefined, context,
    { Ctor: Ctor, propsData: propsData, listeners: listeners, tag: tag, children: children },
    asyncFactory
  );

  return vnode
}

function createComponentInstanceForVnode (
  // we know it's MountedComponentVNode but flow doesn't
  vnode,
  // activeInstance in lifecycle state
  parent
) {
  var options = {
    _isComponent: true,
    _parentVnode: vnode,
    parent: parent
  };
  // check inline-template render functions
  var inlineTemplate = vnode.data.inlineTemplate;
  if (isDef(inlineTemplate)) {
    options.render = inlineTemplate.render;
    options.staticRenderFns = inlineTemplate.staticRenderFns;
  }
  return new vnode.componentOptions.Ctor(options)
}

function installComponentHooks (data) {
  var hooks = data.hook || (data.hook = {});
  for (var i = 0; i < hooksToMerge.length; i++) {
    var key = hooksToMerge[i];
    var existing = hooks[key];
    var toMerge = componentVNodeHooks[key];
    if (existing !== toMerge && !(existing && existing._merged)) {
      hooks[key] = existing ? mergeHook$1(toMerge, existing) : toMerge;
    }
  }
}

function mergeHook$1 (f1, f2) {
  var merged = function (a, b) {
    // flow complains about extra args which is why we use any
    f1(a, b);
    f2(a, b);
  };
  merged._merged = true;
  return merged
}

// transform component v-model info (value and callback) into
// prop and event handler respectively.
function transformModel (options, data) {
  var prop = (options.model && options.model.prop) || 'value';
  var event = (options.model && options.model.event) || 'input'
  ;(data.attrs || (data.attrs = {}))[prop] = data.model.value;
  var on = data.on || (data.on = {});
  var existing = on[event];
  var callback = data.model.callback;
  if (isDef(existing)) {
    if (
      Array.isArray(existing)
        ? existing.indexOf(callback) === -1
        : existing !== callback
    ) {
      on[event] = [callback].concat(existing);
    }
  } else {
    on[event] = callback;
  }
}

/*  */

var SIMPLE_NORMALIZE = 1;
var ALWAYS_NORMALIZE = 2;

// wrapper function for providing a more flexible interface
// without getting yelled at by flow
function createElement (
  context,
  tag,
  data,
  children,
  normalizationType,
  alwaysNormalize
) {
  if (Array.isArray(data) || isPrimitive(data)) {
    normalizationType = children;
    children = data;
    data = undefined;
  }
  if (isTrue(alwaysNormalize)) {
    normalizationType = ALWAYS_NORMALIZE;
  }
  return _createElement(context, tag, data, children, normalizationType)
}

function _createElement (
  context,
  tag,
  data,
  children,
  normalizationType
) {
  if (isDef(data) && isDef((data).__ob__)) {
     true && warn(
      "Avoid using observed data object as vnode data: " + (JSON.stringify(data)) + "\n" +
      'Always create fresh vnode data objects in each render!',
      context
    );
    return createEmptyVNode()
  }
  // object syntax in v-bind
  if (isDef(data) && isDef(data.is)) {
    tag = data.is;
  }
  if (!tag) {
    // in case of component :is set to falsy value
    return createEmptyVNode()
  }
  // warn against non-primitive key
  if ( true &&
    isDef(data) && isDef(data.key) && !isPrimitive(data.key)
  ) {
    {
      warn(
        'Avoid using non-primitive value as key, ' +
        'use string/number value instead.',
        context
      );
    }
  }
  // support single function children as default scoped slot
  if (Array.isArray(children) &&
    typeof children[0] === 'function'
  ) {
    data = data || {};
    data.scopedSlots = { default: children[0] };
    children.length = 0;
  }
  if (normalizationType === ALWAYS_NORMALIZE) {
    children = normalizeChildren(children);
  } else if (normalizationType === SIMPLE_NORMALIZE) {
    children = simpleNormalizeChildren(children);
  }
  var vnode, ns;
  if (typeof tag === 'string') {
    var Ctor;
    ns = (context.$vnode && context.$vnode.ns) || config.getTagNamespace(tag);
    if (config.isReservedTag(tag)) {
      // platform built-in elements
      if ( true && isDef(data) && isDef(data.nativeOn) && data.tag !== 'component') {
        warn(
          ("The .native modifier for v-on is only valid on components but it was used on <" + tag + ">."),
          context
        );
      }
      vnode = new VNode(
        config.parsePlatformTagName(tag), data, children,
        undefined, undefined, context
      );
    } else if ((!data || !data.pre) && isDef(Ctor = resolveAsset(context.$options, 'components', tag))) {
      // component
      vnode = createComponent(Ctor, data, context, children, tag);
    } else {
      // unknown or unlisted namespaced elements
      // check at runtime because it may get assigned a namespace when its
      // parent normalizes children
      vnode = new VNode(
        tag, data, children,
        undefined, undefined, context
      );
    }
  } else {
    // direct component options / constructor
    vnode = createComponent(tag, data, context, children);
  }
  if (Array.isArray(vnode)) {
    return vnode
  } else if (isDef(vnode)) {
    if (isDef(ns)) { applyNS(vnode, ns); }
    if (isDef(data)) { registerDeepBindings(data); }
    return vnode
  } else {
    return createEmptyVNode()
  }
}

function applyNS (vnode, ns, force) {
  vnode.ns = ns;
  if (vnode.tag === 'foreignObject') {
    // use default namespace inside foreignObject
    ns = undefined;
    force = true;
  }
  if (isDef(vnode.children)) {
    for (var i = 0, l = vnode.children.length; i < l; i++) {
      var child = vnode.children[i];
      if (isDef(child.tag) && (
        isUndef(child.ns) || (isTrue(force) && child.tag !== 'svg'))) {
        applyNS(child, ns, force);
      }
    }
  }
}

// ref #5318
// necessary to ensure parent re-render when deep bindings like :style and
// :class are used on slot nodes
function registerDeepBindings (data) {
  if (isObject(data.style)) {
    traverse(data.style);
  }
  if (isObject(data.class)) {
    traverse(data.class);
  }
}

/*  */

function initRender (vm) {
  vm._vnode = null; // the root of the child tree
  vm._staticTrees = null; // v-once cached trees
  var options = vm.$options;
  var parentVnode = vm.$vnode = options._parentVnode; // the placeholder node in parent tree
  var renderContext = parentVnode && parentVnode.context;
  vm.$slots = resolveSlots(options._renderChildren, renderContext);
  vm.$scopedSlots = emptyObject;
  // bind the createElement fn to this instance
  // so that we get proper render context inside it.
  // args order: tag, data, children, normalizationType, alwaysNormalize
  // internal version is used by render functions compiled from templates
  vm._c = function (a, b, c, d) { return createElement(vm, a, b, c, d, false); };
  // normalization is always applied for the public version, used in
  // user-written render functions.
  vm.$createElement = function (a, b, c, d) { return createElement(vm, a, b, c, d, true); };

  // $attrs & $listeners are exposed for easier HOC creation.
  // they need to be reactive so that HOCs using them are always updated
  var parentData = parentVnode && parentVnode.data;

  /* istanbul ignore else */
  if (true) {
    defineReactive$$1(vm, '$attrs', parentData && parentData.attrs || emptyObject, function () {
      !isUpdatingChildComponent && warn("$attrs is readonly.", vm);
    }, true);
    defineReactive$$1(vm, '$listeners', options._parentListeners || emptyObject, function () {
      !isUpdatingChildComponent && warn("$listeners is readonly.", vm);
    }, true);
  } else {}
}

var currentRenderingInstance = null;

function renderMixin (Vue) {
  // install runtime convenience helpers
  installRenderHelpers(Vue.prototype);

  Vue.prototype.$nextTick = function (fn) {
    return nextTick(fn, this)
  };

  Vue.prototype._render = function () {
    var vm = this;
    var ref = vm.$options;
    var render = ref.render;
    var _parentVnode = ref._parentVnode;

    if (_parentVnode) {
      vm.$scopedSlots = normalizeScopedSlots(
        _parentVnode.data.scopedSlots,
        vm.$slots,
        vm.$scopedSlots
      );
    }

    // set parent vnode. this allows render functions to have access
    // to the data on the placeholder node.
    vm.$vnode = _parentVnode;
    // render self
    var vnode;
    try {
      // There's no need to maintain a stack because all render fns are called
      // separately from one another. Nested component's render fns are called
      // when parent component is patched.
      currentRenderingInstance = vm;
      vnode = render.call(vm._renderProxy, vm.$createElement);
    } catch (e) {
      handleError(e, vm, "render");
      // return error render result,
      // or previous vnode to prevent render error causing blank component
      /* istanbul ignore else */
      if ( true && vm.$options.renderError) {
        try {
          vnode = vm.$options.renderError.call(vm._renderProxy, vm.$createElement, e);
        } catch (e) {
          handleError(e, vm, "renderError");
          vnode = vm._vnode;
        }
      } else {
        vnode = vm._vnode;
      }
    } finally {
      currentRenderingInstance = null;
    }
    // if the returned array contains only a single node, allow it
    if (Array.isArray(vnode) && vnode.length === 1) {
      vnode = vnode[0];
    }
    // return empty vnode in case the render function errored out
    if (!(vnode instanceof VNode)) {
      if ( true && Array.isArray(vnode)) {
        warn(
          'Multiple root nodes returned from render function. Render function ' +
          'should return a single root node.',
          vm
        );
      }
      vnode = createEmptyVNode();
    }
    // set parent
    vnode.parent = _parentVnode;
    return vnode
  };
}

/*  */

function ensureCtor (comp, base) {
  if (
    comp.__esModule ||
    (hasSymbol && comp[Symbol.toStringTag] === 'Module')
  ) {
    comp = comp.default;
  }
  return isObject(comp)
    ? base.extend(comp)
    : comp
}

function createAsyncPlaceholder (
  factory,
  data,
  context,
  children,
  tag
) {
  var node = createEmptyVNode();
  node.asyncFactory = factory;
  node.asyncMeta = { data: data, context: context, children: children, tag: tag };
  return node
}

function resolveAsyncComponent (
  factory,
  baseCtor
) {
  if (isTrue(factory.error) && isDef(factory.errorComp)) {
    return factory.errorComp
  }

  if (isDef(factory.resolved)) {
    return factory.resolved
  }

  var owner = currentRenderingInstance;
  if (owner && isDef(factory.owners) && factory.owners.indexOf(owner) === -1) {
    // already pending
    factory.owners.push(owner);
  }

  if (isTrue(factory.loading) && isDef(factory.loadingComp)) {
    return factory.loadingComp
  }

  if (owner && !isDef(factory.owners)) {
    var owners = factory.owners = [owner];
    var sync = true;
    var timerLoading = null;
    var timerTimeout = null

    ;(owner).$on('hook:destroyed', function () { return remove(owners, owner); });

    var forceRender = function (renderCompleted) {
      for (var i = 0, l = owners.length; i < l; i++) {
        (owners[i]).$forceUpdate();
      }

      if (renderCompleted) {
        owners.length = 0;
        if (timerLoading !== null) {
          clearTimeout(timerLoading);
          timerLoading = null;
        }
        if (timerTimeout !== null) {
          clearTimeout(timerTimeout);
          timerTimeout = null;
        }
      }
    };

    var resolve = once(function (res) {
      // cache resolved
      factory.resolved = ensureCtor(res, baseCtor);
      // invoke callbacks only if this is not a synchronous resolve
      // (async resolves are shimmed as synchronous during SSR)
      if (!sync) {
        forceRender(true);
      } else {
        owners.length = 0;
      }
    });

    var reject = once(function (reason) {
       true && warn(
        "Failed to resolve async component: " + (String(factory)) +
        (reason ? ("\nReason: " + reason) : '')
      );
      if (isDef(factory.errorComp)) {
        factory.error = true;
        forceRender(true);
      }
    });

    var res = factory(resolve, reject);

    if (isObject(res)) {
      if (isPromise(res)) {
        // () => Promise
        if (isUndef(factory.resolved)) {
          res.then(resolve, reject);
        }
      } else if (isPromise(res.component)) {
        res.component.then(resolve, reject);

        if (isDef(res.error)) {
          factory.errorComp = ensureCtor(res.error, baseCtor);
        }

        if (isDef(res.loading)) {
          factory.loadingComp = ensureCtor(res.loading, baseCtor);
          if (res.delay === 0) {
            factory.loading = true;
          } else {
            timerLoading = setTimeout(function () {
              timerLoading = null;
              if (isUndef(factory.resolved) && isUndef(factory.error)) {
                factory.loading = true;
                forceRender(false);
              }
            }, res.delay || 200);
          }
        }

        if (isDef(res.timeout)) {
          timerTimeout = setTimeout(function () {
            timerTimeout = null;
            if (isUndef(factory.resolved)) {
              reject(
                 true
                  ? ("timeout (" + (res.timeout) + "ms)")
                  : 0
              );
            }
          }, res.timeout);
        }
      }
    }

    sync = false;
    // return in case resolved synchronously
    return factory.loading
      ? factory.loadingComp
      : factory.resolved
  }
}

/*  */

function getFirstComponentChild (children) {
  if (Array.isArray(children)) {
    for (var i = 0; i < children.length; i++) {
      var c = children[i];
      if (isDef(c) && (isDef(c.componentOptions) || isAsyncPlaceholder(c))) {
        return c
      }
    }
  }
}

/*  */

/*  */

function initEvents (vm) {
  vm._events = Object.create(null);
  vm._hasHookEvent = false;
  // init parent attached events
  var listeners = vm.$options._parentListeners;
  if (listeners) {
    updateComponentListeners(vm, listeners);
  }
}

var target;

function add (event, fn) {
  target.$on(event, fn);
}

function remove$1 (event, fn) {
  target.$off(event, fn);
}

function createOnceHandler (event, fn) {
  var _target = target;
  return function onceHandler () {
    var res = fn.apply(null, arguments);
    if (res !== null) {
      _target.$off(event, onceHandler);
    }
  }
}

function updateComponentListeners (
  vm,
  listeners,
  oldListeners
) {
  target = vm;
  updateListeners(listeners, oldListeners || {}, add, remove$1, createOnceHandler, vm);
  target = undefined;
}

function eventsMixin (Vue) {
  var hookRE = /^hook:/;
  Vue.prototype.$on = function (event, fn) {
    var vm = this;
    if (Array.isArray(event)) {
      for (var i = 0, l = event.length; i < l; i++) {
        vm.$on(event[i], fn);
      }
    } else {
      (vm._events[event] || (vm._events[event] = [])).push(fn);
      // optimize hook:event cost by using a boolean flag marked at registration
      // instead of a hash lookup
      if (hookRE.test(event)) {
        vm._hasHookEvent = true;
      }
    }
    return vm
  };

  Vue.prototype.$once = function (event, fn) {
    var vm = this;
    function on () {
      vm.$off(event, on);
      fn.apply(vm, arguments);
    }
    on.fn = fn;
    vm.$on(event, on);
    return vm
  };

  Vue.prototype.$off = function (event, fn) {
    var vm = this;
    // all
    if (!arguments.length) {
      vm._events = Object.create(null);
      return vm
    }
    // array of events
    if (Array.isArray(event)) {
      for (var i$1 = 0, l = event.length; i$1 < l; i$1++) {
        vm.$off(event[i$1], fn);
      }
      return vm
    }
    // specific event
    var cbs = vm._events[event];
    if (!cbs) {
      return vm
    }
    if (!fn) {
      vm._events[event] = null;
      return vm
    }
    // specific handler
    var cb;
    var i = cbs.length;
    while (i--) {
      cb = cbs[i];
      if (cb === fn || cb.fn === fn) {
        cbs.splice(i, 1);
        break
      }
    }
    return vm
  };

  Vue.prototype.$emit = function (event) {
    var vm = this;
    if (true) {
      var lowerCaseEvent = event.toLowerCase();
      if (lowerCaseEvent !== event && vm._events[lowerCaseEvent]) {
        tip(
          "Event \"" + lowerCaseEvent + "\" is emitted in component " +
          (formatComponentName(vm)) + " but the handler is registered for \"" + event + "\". " +
          "Note that HTML attributes are case-insensitive and you cannot use " +
          "v-on to listen to camelCase events when using in-DOM templates. " +
          "You should probably use \"" + (hyphenate(event)) + "\" instead of \"" + event + "\"."
        );
      }
    }
    var cbs = vm._events[event];
    if (cbs) {
      cbs = cbs.length > 1 ? toArray(cbs) : cbs;
      var args = toArray(arguments, 1);
      var info = "event handler for \"" + event + "\"";
      for (var i = 0, l = cbs.length; i < l; i++) {
        invokeWithErrorHandling(cbs[i], vm, args, vm, info);
      }
    }
    return vm
  };
}

/*  */

var activeInstance = null;
var isUpdatingChildComponent = false;

function setActiveInstance(vm) {
  var prevActiveInstance = activeInstance;
  activeInstance = vm;
  return function () {
    activeInstance = prevActiveInstance;
  }
}

function initLifecycle (vm) {
  var options = vm.$options;

  // locate first non-abstract parent
  var parent = options.parent;
  if (parent && !options.abstract) {
    while (parent.$options.abstract && parent.$parent) {
      parent = parent.$parent;
    }
    parent.$children.push(vm);
  }

  vm.$parent = parent;
  vm.$root = parent ? parent.$root : vm;

  vm.$children = [];
  vm.$refs = {};

  vm._watcher = null;
  vm._inactive = null;
  vm._directInactive = false;
  vm._isMounted = false;
  vm._isDestroyed = false;
  vm._isBeingDestroyed = false;
}

function lifecycleMixin (Vue) {
  Vue.prototype._update = function (vnode, hydrating) {
    var vm = this;
    var prevEl = vm.$el;
    var prevVnode = vm._vnode;
    var restoreActiveInstance = setActiveInstance(vm);
    vm._vnode = vnode;
    // Vue.prototype.__patch__ is injected in entry points
    // based on the rendering backend used.
    if (!prevVnode) {
      // initial render
      vm.$el = vm.__patch__(vm.$el, vnode, hydrating, false /* removeOnly */);
    } else {
      // updates
      vm.$el = vm.__patch__(prevVnode, vnode);
    }
    restoreActiveInstance();
    // update __vue__ reference
    if (prevEl) {
      prevEl.__vue__ = null;
    }
    if (vm.$el) {
      vm.$el.__vue__ = vm;
    }
    // if parent is an HOC, update its $el as well
    if (vm.$vnode && vm.$parent && vm.$vnode === vm.$parent._vnode) {
      vm.$parent.$el = vm.$el;
    }
    // updated hook is called by the scheduler to ensure that children are
    // updated in a parent's updated hook.
  };

  Vue.prototype.$forceUpdate = function () {
    var vm = this;
    if (vm._watcher) {
      vm._watcher.update();
    }
  };

  Vue.prototype.$destroy = function () {
    var vm = this;
    if (vm._isBeingDestroyed) {
      return
    }
    callHook(vm, 'beforeDestroy');
    vm._isBeingDestroyed = true;
    // remove self from parent
    var parent = vm.$parent;
    if (parent && !parent._isBeingDestroyed && !vm.$options.abstract) {
      remove(parent.$children, vm);
    }
    // teardown watchers
    if (vm._watcher) {
      vm._watcher.teardown();
    }
    var i = vm._watchers.length;
    while (i--) {
      vm._watchers[i].teardown();
    }
    // remove reference from data ob
    // frozen object may not have observer.
    if (vm._data.__ob__) {
      vm._data.__ob__.vmCount--;
    }
    // call the last hook...
    vm._isDestroyed = true;
    // invoke destroy hooks on current rendered tree
    vm.__patch__(vm._vnode, null);
    // fire destroyed hook
    callHook(vm, 'destroyed');
    // turn off all instance listeners.
    vm.$off();
    // remove __vue__ reference
    if (vm.$el) {
      vm.$el.__vue__ = null;
    }
    // release circular reference (#6759)
    if (vm.$vnode) {
      vm.$vnode.parent = null;
    }
  };
}

function mountComponent (
  vm,
  el,
  hydrating
) {
  vm.$el = el;
  if (!vm.$options.render) {
    vm.$options.render = createEmptyVNode;
    if (true) {
      /* istanbul ignore if */
      if ((vm.$options.template && vm.$options.template.charAt(0) !== '#') ||
        vm.$options.el || el) {
        warn(
          'You are using the runtime-only build of Vue where the template ' +
          'compiler is not available. Either pre-compile the templates into ' +
          'render functions, or use the compiler-included build.',
          vm
        );
      } else {
        warn(
          'Failed to mount component: template or render function not defined.',
          vm
        );
      }
    }
  }
  callHook(vm, 'beforeMount');

  var updateComponent;
  /* istanbul ignore if */
  if ( true && config.performance && mark) {
    updateComponent = function () {
      var name = vm._name;
      var id = vm._uid;
      var startTag = "vue-perf-start:" + id;
      var endTag = "vue-perf-end:" + id;

      mark(startTag);
      var vnode = vm._render();
      mark(endTag);
      measure(("vue " + name + " render"), startTag, endTag);

      mark(startTag);
      vm._update(vnode, hydrating);
      mark(endTag);
      measure(("vue " + name + " patch"), startTag, endTag);
    };
  } else {
    updateComponent = function () {
      vm._update(vm._render(), hydrating);
    };
  }

  // we set this to vm._watcher inside the watcher's constructor
  // since the watcher's initial patch may call $forceUpdate (e.g. inside child
  // component's mounted hook), which relies on vm._watcher being already defined
  new Watcher(vm, updateComponent, noop, {
    before: function before () {
      if (vm._isMounted && !vm._isDestroyed) {
        callHook(vm, 'beforeUpdate');
      }
    }
  }, true /* isRenderWatcher */);
  hydrating = false;

  // manually mounted instance, call mounted on self
  // mounted is called for render-created child components in its inserted hook
  if (vm.$vnode == null) {
    vm._isMounted = true;
    callHook(vm, 'mounted');
  }
  return vm
}

function updateChildComponent (
  vm,
  propsData,
  listeners,
  parentVnode,
  renderChildren
) {
  if (true) {
    isUpdatingChildComponent = true;
  }

  // determine whether component has slot children
  // we need to do this before overwriting $options._renderChildren.

  // check if there are dynamic scopedSlots (hand-written or compiled but with
  // dynamic slot names). Static scoped slots compiled from template has the
  // "$stable" marker.
  var newScopedSlots = parentVnode.data.scopedSlots;
  var oldScopedSlots = vm.$scopedSlots;
  var hasDynamicScopedSlot = !!(
    (newScopedSlots && !newScopedSlots.$stable) ||
    (oldScopedSlots !== emptyObject && !oldScopedSlots.$stable) ||
    (newScopedSlots && vm.$scopedSlots.$key !== newScopedSlots.$key) ||
    (!newScopedSlots && vm.$scopedSlots.$key)
  );

  // Any static slot children from the parent may have changed during parent's
  // update. Dynamic scoped slots may also have changed. In such cases, a forced
  // update is necessary to ensure correctness.
  var needsForceUpdate = !!(
    renderChildren ||               // has new static slots
    vm.$options._renderChildren ||  // has old static slots
    hasDynamicScopedSlot
  );

  vm.$options._parentVnode = parentVnode;
  vm.$vnode = parentVnode; // update vm's placeholder node without re-render

  if (vm._vnode) { // update child tree's parent
    vm._vnode.parent = parentVnode;
  }
  vm.$options._renderChildren = renderChildren;

  // update $attrs and $listeners hash
  // these are also reactive so they may trigger child update if the child
  // used them during render
  vm.$attrs = parentVnode.data.attrs || emptyObject;
  vm.$listeners = listeners || emptyObject;

  // update props
  if (propsData && vm.$options.props) {
    toggleObserving(false);
    var props = vm._props;
    var propKeys = vm.$options._propKeys || [];
    for (var i = 0; i < propKeys.length; i++) {
      var key = propKeys[i];
      var propOptions = vm.$options.props; // wtf flow?
      props[key] = validateProp(key, propOptions, propsData, vm);
    }
    toggleObserving(true);
    // keep a copy of raw propsData
    vm.$options.propsData = propsData;
  }

  // update listeners
  listeners = listeners || emptyObject;
  var oldListeners = vm.$options._parentListeners;
  vm.$options._parentListeners = listeners;
  updateComponentListeners(vm, listeners, oldListeners);

  // resolve slots + force update if has children
  if (needsForceUpdate) {
    vm.$slots = resolveSlots(renderChildren, parentVnode.context);
    vm.$forceUpdate();
  }

  if (true) {
    isUpdatingChildComponent = false;
  }
}

function isInInactiveTree (vm) {
  while (vm && (vm = vm.$parent)) {
    if (vm._inactive) { return true }
  }
  return false
}

function activateChildComponent (vm, direct) {
  if (direct) {
    vm._directInactive = false;
    if (isInInactiveTree(vm)) {
      return
    }
  } else if (vm._directInactive) {
    return
  }
  if (vm._inactive || vm._inactive === null) {
    vm._inactive = false;
    for (var i = 0; i < vm.$children.length; i++) {
      activateChildComponent(vm.$children[i]);
    }
    callHook(vm, 'activated');
  }
}

function deactivateChildComponent (vm, direct) {
  if (direct) {
    vm._directInactive = true;
    if (isInInactiveTree(vm)) {
      return
    }
  }
  if (!vm._inactive) {
    vm._inactive = true;
    for (var i = 0; i < vm.$children.length; i++) {
      deactivateChildComponent(vm.$children[i]);
    }
    callHook(vm, 'deactivated');
  }
}

function callHook (vm, hook) {
  // #7573 disable dep collection when invoking lifecycle hooks
  pushTarget();
  var handlers = vm.$options[hook];
  var info = hook + " hook";
  if (handlers) {
    for (var i = 0, j = handlers.length; i < j; i++) {
      invokeWithErrorHandling(handlers[i], vm, null, vm, info);
    }
  }
  if (vm._hasHookEvent) {
    vm.$emit('hook:' + hook);
  }
  popTarget();
}

/*  */

var MAX_UPDATE_COUNT = 100;

var queue = [];
var activatedChildren = [];
var has = {};
var circular = {};
var waiting = false;
var flushing = false;
var index = 0;

/**
 * Reset the scheduler's state.
 */
function resetSchedulerState () {
  index = queue.length = activatedChildren.length = 0;
  has = {};
  if (true) {
    circular = {};
  }
  waiting = flushing = false;
}

// Async edge case #6566 requires saving the timestamp when event listeners are
// attached. However, calling performance.now() has a perf overhead especially
// if the page has thousands of event listeners. Instead, we take a timestamp
// every time the scheduler flushes and use that for all event listeners
// attached during that flush.
var currentFlushTimestamp = 0;

// Async edge case fix requires storing an event listener's attach timestamp.
var getNow = Date.now;

// Determine what event timestamp the browser is using. Annoyingly, the
// timestamp can either be hi-res (relative to page load) or low-res
// (relative to UNIX epoch), so in order to compare time we have to use the
// same timestamp type when saving the flush timestamp.
// All IE versions use low-res event timestamps, and have problematic clock
// implementations (#9632)
if (inBrowser && !isIE) {
  var performance = window.performance;
  if (
    performance &&
    typeof performance.now === 'function' &&
    getNow() > document.createEvent('Event').timeStamp
  ) {
    // if the event timestamp, although evaluated AFTER the Date.now(), is
    // smaller than it, it means the event is using a hi-res timestamp,
    // and we need to use the hi-res version for event listener timestamps as
    // well.
    getNow = function () { return performance.now(); };
  }
}

/**
 * Flush both queues and run the watchers.
 */
function flushSchedulerQueue () {
  currentFlushTimestamp = getNow();
  flushing = true;
  var watcher, id;

  // Sort queue before flush.
  // This ensures that:
  // 1. Components are updated from parent to child. (because parent is always
  //    created before the child)
  // 2. A component's user watchers are run before its render watcher (because
  //    user watchers are created before the render watcher)
  // 3. If a component is destroyed during a parent component's watcher run,
  //    its watchers can be skipped.
  queue.sort(function (a, b) { return a.id - b.id; });

  // do not cache length because more watchers might be pushed
  // as we run existing watchers
  for (index = 0; index < queue.length; index++) {
    watcher = queue[index];
    if (watcher.before) {
      watcher.before();
    }
    id = watcher.id;
    has[id] = null;
    watcher.run();
    // in dev build, check and stop circular updates.
    if ( true && has[id] != null) {
      circular[id] = (circular[id] || 0) + 1;
      if (circular[id] > MAX_UPDATE_COUNT) {
        warn(
          'You may have an infinite update loop ' + (
            watcher.user
              ? ("in watcher with expression \"" + (watcher.expression) + "\"")
              : "in a component render function."
          ),
          watcher.vm
        );
        break
      }
    }
  }

  // keep copies of post queues before resetting state
  var activatedQueue = activatedChildren.slice();
  var updatedQueue = queue.slice();

  resetSchedulerState();

  // call component updated and activated hooks
  callActivatedHooks(activatedQueue);
  callUpdatedHooks(updatedQueue);

  // devtool hook
  /* istanbul ignore if */
  if (devtools && config.devtools) {
    devtools.emit('flush');
  }
}

function callUpdatedHooks (queue) {
  var i = queue.length;
  while (i--) {
    var watcher = queue[i];
    var vm = watcher.vm;
    if (vm._watcher === watcher && vm._isMounted && !vm._isDestroyed) {
      callHook(vm, 'updated');
    }
  }
}

/**
 * Queue a kept-alive component that was activated during patch.
 * The queue will be processed after the entire tree has been patched.
 */
function queueActivatedComponent (vm) {
  // setting _inactive to false here so that a render function can
  // rely on checking whether it's in an inactive tree (e.g. router-view)
  vm._inactive = false;
  activatedChildren.push(vm);
}

function callActivatedHooks (queue) {
  for (var i = 0; i < queue.length; i++) {
    queue[i]._inactive = true;
    activateChildComponent(queue[i], true /* true */);
  }
}

/**
 * Push a watcher into the watcher queue.
 * Jobs with duplicate IDs will be skipped unless it's
 * pushed when the queue is being flushed.
 */
function queueWatcher (watcher) {
  var id = watcher.id;
  if (has[id] == null) {
    has[id] = true;
    if (!flushing) {
      queue.push(watcher);
    } else {
      // if already flushing, splice the watcher based on its id
      // if already past its id, it will be run next immediately.
      var i = queue.length - 1;
      while (i > index && queue[i].id > watcher.id) {
        i--;
      }
      queue.splice(i + 1, 0, watcher);
    }
    // queue the flush
    if (!waiting) {
      waiting = true;

      if ( true && !config.async) {
        flushSchedulerQueue();
        return
      }
      nextTick(flushSchedulerQueue);
    }
  }
}

/*  */



var uid$2 = 0;

/**
 * A watcher parses an expression, collects dependencies,
 * and fires callback when the expression value changes.
 * This is used for both the $watch() api and directives.
 */
var Watcher = function Watcher (
  vm,
  expOrFn,
  cb,
  options,
  isRenderWatcher
) {
  this.vm = vm;
  if (isRenderWatcher) {
    vm._watcher = this;
  }
  vm._watchers.push(this);
  // options
  if (options) {
    this.deep = !!options.deep;
    this.user = !!options.user;
    this.lazy = !!options.lazy;
    this.sync = !!options.sync;
    this.before = options.before;
  } else {
    this.deep = this.user = this.lazy = this.sync = false;
  }
  this.cb = cb;
  this.id = ++uid$2; // uid for batching
  this.active = true;
  this.dirty = this.lazy; // for lazy watchers
  this.deps = [];
  this.newDeps = [];
  this.depIds = new _Set();
  this.newDepIds = new _Set();
  this.expression =  true
    ? expOrFn.toString()
    : 0;
  // parse expression for getter
  if (typeof expOrFn === 'function') {
    this.getter = expOrFn;
  } else {
    this.getter = parsePath(expOrFn);
    if (!this.getter) {
      this.getter = noop;
       true && warn(
        "Failed watching path: \"" + expOrFn + "\" " +
        'Watcher only accepts simple dot-delimited paths. ' +
        'For full control, use a function instead.',
        vm
      );
    }
  }
  this.value = this.lazy
    ? undefined
    : this.get();
};

/**
 * Evaluate the getter, and re-collect dependencies.
 */
Watcher.prototype.get = function get () {
  pushTarget(this);
  var value;
  var vm = this.vm;
  try {
    value = this.getter.call(vm, vm);
  } catch (e) {
    if (this.user) {
      handleError(e, vm, ("getter for watcher \"" + (this.expression) + "\""));
    } else {
      throw e
    }
  } finally {
    // "touch" every property so they are all tracked as
    // dependencies for deep watching
    if (this.deep) {
      traverse(value);
    }
    popTarget();
    this.cleanupDeps();
  }
  return value
};

/**
 * Add a dependency to this directive.
 */
Watcher.prototype.addDep = function addDep (dep) {
  var id = dep.id;
  if (!this.newDepIds.has(id)) {
    this.newDepIds.add(id);
    this.newDeps.push(dep);
    if (!this.depIds.has(id)) {
      dep.addSub(this);
    }
  }
};

/**
 * Clean up for dependency collection.
 */
Watcher.prototype.cleanupDeps = function cleanupDeps () {
  var i = this.deps.length;
  while (i--) {
    var dep = this.deps[i];
    if (!this.newDepIds.has(dep.id)) {
      dep.removeSub(this);
    }
  }
  var tmp = this.depIds;
  this.depIds = this.newDepIds;
  this.newDepIds = tmp;
  this.newDepIds.clear();
  tmp = this.deps;
  this.deps = this.newDeps;
  this.newDeps = tmp;
  this.newDeps.length = 0;
};

/**
 * Subscriber interface.
 * Will be called when a dependency changes.
 */
Watcher.prototype.update = function update () {
  /* istanbul ignore else */
  if (this.lazy) {
    this.dirty = true;
  } else if (this.sync) {
    this.run();
  } else {
    queueWatcher(this);
  }
};

/**
 * Scheduler job interface.
 * Will be called by the scheduler.
 */
Watcher.prototype.run = function run () {
  if (this.active) {
    var value = this.get();
    if (
      value !== this.value ||
      // Deep watchers and watchers on Object/Arrays should fire even
      // when the value is the same, because the value may
      // have mutated.
      isObject(value) ||
      this.deep
    ) {
      // set new value
      var oldValue = this.value;
      this.value = value;
      if (this.user) {
        var info = "callback for watcher \"" + (this.expression) + "\"";
        invokeWithErrorHandling(this.cb, this.vm, [value, oldValue], this.vm, info);
      } else {
        this.cb.call(this.vm, value, oldValue);
      }
    }
  }
};

/**
 * Evaluate the value of the watcher.
 * This only gets called for lazy watchers.
 */
Watcher.prototype.evaluate = function evaluate () {
  this.value = this.get();
  this.dirty = false;
};

/**
 * Depend on all deps collected by this watcher.
 */
Watcher.prototype.depend = function depend () {
  var i = this.deps.length;
  while (i--) {
    this.deps[i].depend();
  }
};

/**
 * Remove self from all dependencies' subscriber list.
 */
Watcher.prototype.teardown = function teardown () {
  if (this.active) {
    // remove self from vm's watcher list
    // this is a somewhat expensive operation so we skip it
    // if the vm is being destroyed.
    if (!this.vm._isBeingDestroyed) {
      remove(this.vm._watchers, this);
    }
    var i = this.deps.length;
    while (i--) {
      this.deps[i].removeSub(this);
    }
    this.active = false;
  }
};

/*  */

var sharedPropertyDefinition = {
  enumerable: true,
  configurable: true,
  get: noop,
  set: noop
};

function proxy (target, sourceKey, key) {
  sharedPropertyDefinition.get = function proxyGetter () {
    return this[sourceKey][key]
  };
  sharedPropertyDefinition.set = function proxySetter (val) {
    this[sourceKey][key] = val;
  };
  Object.defineProperty(target, key, sharedPropertyDefinition);
}

function initState (vm) {
  vm._watchers = [];
  var opts = vm.$options;
  if (opts.props) { initProps(vm, opts.props); }
  if (opts.methods) { initMethods(vm, opts.methods); }
  if (opts.data) {
    initData(vm);
  } else {
    observe(vm._data = {}, true /* asRootData */);
  }
  if (opts.computed) { initComputed(vm, opts.computed); }
  if (opts.watch && opts.watch !== nativeWatch) {
    initWatch(vm, opts.watch);
  }
}

function initProps (vm, propsOptions) {
  var propsData = vm.$options.propsData || {};
  var props = vm._props = {};
  // cache prop keys so that future props updates can iterate using Array
  // instead of dynamic object key enumeration.
  var keys = vm.$options._propKeys = [];
  var isRoot = !vm.$parent;
  // root instance props should be converted
  if (!isRoot) {
    toggleObserving(false);
  }
  var loop = function ( key ) {
    keys.push(key);
    var value = validateProp(key, propsOptions, propsData, vm);
    /* istanbul ignore else */
    if (true) {
      var hyphenatedKey = hyphenate(key);
      if (isReservedAttribute(hyphenatedKey) ||
          config.isReservedAttr(hyphenatedKey)) {
        warn(
          ("\"" + hyphenatedKey + "\" is a reserved attribute and cannot be used as component prop."),
          vm
        );
      }
      defineReactive$$1(props, key, value, function () {
        if (!isRoot && !isUpdatingChildComponent) {
          warn(
            "Avoid mutating a prop directly since the value will be " +
            "overwritten whenever the parent component re-renders. " +
            "Instead, use a data or computed property based on the prop's " +
            "value. Prop being mutated: \"" + key + "\"",
            vm
          );
        }
      });
    } else {}
    // static props are already proxied on the component's prototype
    // during Vue.extend(). We only need to proxy props defined at
    // instantiation here.
    if (!(key in vm)) {
      proxy(vm, "_props", key);
    }
  };

  for (var key in propsOptions) loop( key );
  toggleObserving(true);
}

function initData (vm) {
  var data = vm.$options.data;
  data = vm._data = typeof data === 'function'
    ? getData(data, vm)
    : data || {};
  if (!isPlainObject(data)) {
    data = {};
     true && warn(
      'data functions should return an object:\n' +
      'https://vuejs.org/v2/guide/components.html#data-Must-Be-a-Function',
      vm
    );
  }
  // proxy data on instance
  var keys = Object.keys(data);
  var props = vm.$options.props;
  var methods = vm.$options.methods;
  var i = keys.length;
  while (i--) {
    var key = keys[i];
    if (true) {
      if (methods && hasOwn(methods, key)) {
        warn(
          ("Method \"" + key + "\" has already been defined as a data property."),
          vm
        );
      }
    }
    if (props && hasOwn(props, key)) {
       true && warn(
        "The data property \"" + key + "\" is already declared as a prop. " +
        "Use prop default value instead.",
        vm
      );
    } else if (!isReserved(key)) {
      proxy(vm, "_data", key);
    }
  }
  // observe data
  observe(data, true /* asRootData */);
}

function getData (data, vm) {
  // #7573 disable dep collection when invoking data getters
  pushTarget();
  try {
    return data.call(vm, vm)
  } catch (e) {
    handleError(e, vm, "data()");
    return {}
  } finally {
    popTarget();
  }
}

var computedWatcherOptions = { lazy: true };

function initComputed (vm, computed) {
  // $flow-disable-line
  var watchers = vm._computedWatchers = Object.create(null);
  // computed properties are just getters during SSR
  var isSSR = isServerRendering();

  for (var key in computed) {
    var userDef = computed[key];
    var getter = typeof userDef === 'function' ? userDef : userDef.get;
    if ( true && getter == null) {
      warn(
        ("Getter is missing for computed property \"" + key + "\"."),
        vm
      );
    }

    if (!isSSR) {
      // create internal watcher for the computed property.
      watchers[key] = new Watcher(
        vm,
        getter || noop,
        noop,
        computedWatcherOptions
      );
    }

    // component-defined computed properties are already defined on the
    // component prototype. We only need to define computed properties defined
    // at instantiation here.
    if (!(key in vm)) {
      defineComputed(vm, key, userDef);
    } else if (true) {
      if (key in vm.$data) {
        warn(("The computed property \"" + key + "\" is already defined in data."), vm);
      } else if (vm.$options.props && key in vm.$options.props) {
        warn(("The computed property \"" + key + "\" is already defined as a prop."), vm);
      } else if (vm.$options.methods && key in vm.$options.methods) {
        warn(("The computed property \"" + key + "\" is already defined as a method."), vm);
      }
    }
  }
}

function defineComputed (
  target,
  key,
  userDef
) {
  var shouldCache = !isServerRendering();
  if (typeof userDef === 'function') {
    sharedPropertyDefinition.get = shouldCache
      ? createComputedGetter(key)
      : createGetterInvoker(userDef);
    sharedPropertyDefinition.set = noop;
  } else {
    sharedPropertyDefinition.get = userDef.get
      ? shouldCache && userDef.cache !== false
        ? createComputedGetter(key)
        : createGetterInvoker(userDef.get)
      : noop;
    sharedPropertyDefinition.set = userDef.set || noop;
  }
  if ( true &&
      sharedPropertyDefinition.set === noop) {
    sharedPropertyDefinition.set = function () {
      warn(
        ("Computed property \"" + key + "\" was assigned to but it has no setter."),
        this
      );
    };
  }
  Object.defineProperty(target, key, sharedPropertyDefinition);
}

function createComputedGetter (key) {
  return function computedGetter () {
    var watcher = this._computedWatchers && this._computedWatchers[key];
    if (watcher) {
      if (watcher.dirty) {
        watcher.evaluate();
      }
      if (Dep.target) {
        watcher.depend();
      }
      return watcher.value
    }
  }
}

function createGetterInvoker(fn) {
  return function computedGetter () {
    return fn.call(this, this)
  }
}

function initMethods (vm, methods) {
  var props = vm.$options.props;
  for (var key in methods) {
    if (true) {
      if (typeof methods[key] !== 'function') {
        warn(
          "Method \"" + key + "\" has type \"" + (typeof methods[key]) + "\" in the component definition. " +
          "Did you reference the function correctly?",
          vm
        );
      }
      if (props && hasOwn(props, key)) {
        warn(
          ("Method \"" + key + "\" has already been defined as a prop."),
          vm
        );
      }
      if ((key in vm) && isReserved(key)) {
        warn(
          "Method \"" + key + "\" conflicts with an existing Vue instance method. " +
          "Avoid defining component methods that start with _ or $."
        );
      }
    }
    vm[key] = typeof methods[key] !== 'function' ? noop : bind(methods[key], vm);
  }
}

function initWatch (vm, watch) {
  for (var key in watch) {
    var handler = watch[key];
    if (Array.isArray(handler)) {
      for (var i = 0; i < handler.length; i++) {
        createWatcher(vm, key, handler[i]);
      }
    } else {
      createWatcher(vm, key, handler);
    }
  }
}

function createWatcher (
  vm,
  expOrFn,
  handler,
  options
) {
  if (isPlainObject(handler)) {
    options = handler;
    handler = handler.handler;
  }
  if (typeof handler === 'string') {
    handler = vm[handler];
  }
  return vm.$watch(expOrFn, handler, options)
}

function stateMixin (Vue) {
  // flow somehow has problems with directly declared definition object
  // when using Object.defineProperty, so we have to procedurally build up
  // the object here.
  var dataDef = {};
  dataDef.get = function () { return this._data };
  var propsDef = {};
  propsDef.get = function () { return this._props };
  if (true) {
    dataDef.set = function () {
      warn(
        'Avoid replacing instance root $data. ' +
        'Use nested data properties instead.',
        this
      );
    };
    propsDef.set = function () {
      warn("$props is readonly.", this);
    };
  }
  Object.defineProperty(Vue.prototype, '$data', dataDef);
  Object.defineProperty(Vue.prototype, '$props', propsDef);

  Vue.prototype.$set = set;
  Vue.prototype.$delete = del;

  Vue.prototype.$watch = function (
    expOrFn,
    cb,
    options
  ) {
    var vm = this;
    if (isPlainObject(cb)) {
      return createWatcher(vm, expOrFn, cb, options)
    }
    options = options || {};
    options.user = true;
    var watcher = new Watcher(vm, expOrFn, cb, options);
    if (options.immediate) {
      var info = "callback for immediate watcher \"" + (watcher.expression) + "\"";
      pushTarget();
      invokeWithErrorHandling(cb, vm, [watcher.value], vm, info);
      popTarget();
    }
    return function unwatchFn () {
      watcher.teardown();
    }
  };
}

/*  */

var uid$3 = 0;

function initMixin (Vue) {
  Vue.prototype._init = function (options) {
    var vm = this;
    // a uid
    vm._uid = uid$3++;

    var startTag, endTag;
    /* istanbul ignore if */
    if ( true && config.performance && mark) {
      startTag = "vue-perf-start:" + (vm._uid);
      endTag = "vue-perf-end:" + (vm._uid);
      mark(startTag);
    }

    // a flag to avoid this being observed
    vm._isVue = true;
    // merge options
    if (options && options._isComponent) {
      // optimize internal component instantiation
      // since dynamic options merging is pretty slow, and none of the
      // internal component options needs special treatment.
      initInternalComponent(vm, options);
    } else {
      vm.$options = mergeOptions(
        resolveConstructorOptions(vm.constructor),
        options || {},
        vm
      );
    }
    /* istanbul ignore else */
    if (true) {
      initProxy(vm);
    } else {}
    // expose real self
    vm._self = vm;
    initLifecycle(vm);
    initEvents(vm);
    initRender(vm);
    callHook(vm, 'beforeCreate');
    initInjections(vm); // resolve injections before data/props
    initState(vm);
    initProvide(vm); // resolve provide after data/props
    callHook(vm, 'created');

    /* istanbul ignore if */
    if ( true && config.performance && mark) {
      vm._name = formatComponentName(vm, false);
      mark(endTag);
      measure(("vue " + (vm._name) + " init"), startTag, endTag);
    }

    if (vm.$options.el) {
      vm.$mount(vm.$options.el);
    }
  };
}

function initInternalComponent (vm, options) {
  var opts = vm.$options = Object.create(vm.constructor.options);
  // doing this because it's faster than dynamic enumeration.
  var parentVnode = options._parentVnode;
  opts.parent = options.parent;
  opts._parentVnode = parentVnode;

  var vnodeComponentOptions = parentVnode.componentOptions;
  opts.propsData = vnodeComponentOptions.propsData;
  opts._parentListeners = vnodeComponentOptions.listeners;
  opts._renderChildren = vnodeComponentOptions.children;
  opts._componentTag = vnodeComponentOptions.tag;

  if (options.render) {
    opts.render = options.render;
    opts.staticRenderFns = options.staticRenderFns;
  }
}

function resolveConstructorOptions (Ctor) {
  var options = Ctor.options;
  if (Ctor.super) {
    var superOptions = resolveConstructorOptions(Ctor.super);
    var cachedSuperOptions = Ctor.superOptions;
    if (superOptions !== cachedSuperOptions) {
      // super option changed,
      // need to resolve new options.
      Ctor.superOptions = superOptions;
      // check if there are any late-modified/attached options (#4976)
      var modifiedOptions = resolveModifiedOptions(Ctor);
      // update base extend options
      if (modifiedOptions) {
        extend(Ctor.extendOptions, modifiedOptions);
      }
      options = Ctor.options = mergeOptions(superOptions, Ctor.extendOptions);
      if (options.name) {
        options.components[options.name] = Ctor;
      }
    }
  }
  return options
}

function resolveModifiedOptions (Ctor) {
  var modified;
  var latest = Ctor.options;
  var sealed = Ctor.sealedOptions;
  for (var key in latest) {
    if (latest[key] !== sealed[key]) {
      if (!modified) { modified = {}; }
      modified[key] = latest[key];
    }
  }
  return modified
}

function Vue (options) {
  if ( true &&
    !(this instanceof Vue)
  ) {
    warn('Vue is a constructor and should be called with the `new` keyword');
  }
  this._init(options);
}

initMixin(Vue);
stateMixin(Vue);
eventsMixin(Vue);
lifecycleMixin(Vue);
renderMixin(Vue);

/*  */

function initUse (Vue) {
  Vue.use = function (plugin) {
    var installedPlugins = (this._installedPlugins || (this._installedPlugins = []));
    if (installedPlugins.indexOf(plugin) > -1) {
      return this
    }

    // additional parameters
    var args = toArray(arguments, 1);
    args.unshift(this);
    if (typeof plugin.install === 'function') {
      plugin.install.apply(plugin, args);
    } else if (typeof plugin === 'function') {
      plugin.apply(null, args);
    }
    installedPlugins.push(plugin);
    return this
  };
}

/*  */

function initMixin$1 (Vue) {
  Vue.mixin = function (mixin) {
    this.options = mergeOptions(this.options, mixin);
    return this
  };
}

/*  */

function initExtend (Vue) {
  /**
   * Each instance constructor, including Vue, has a unique
   * cid. This enables us to create wrapped "child
   * constructors" for prototypal inheritance and cache them.
   */
  Vue.cid = 0;
  var cid = 1;

  /**
   * Class inheritance
   */
  Vue.extend = function (extendOptions) {
    extendOptions = extendOptions || {};
    var Super = this;
    var SuperId = Super.cid;
    var cachedCtors = extendOptions._Ctor || (extendOptions._Ctor = {});
    if (cachedCtors[SuperId]) {
      return cachedCtors[SuperId]
    }

    var name = extendOptions.name || Super.options.name;
    if ( true && name) {
      validateComponentName(name);
    }

    var Sub = function VueComponent (options) {
      this._init(options);
    };
    Sub.prototype = Object.create(Super.prototype);
    Sub.prototype.constructor = Sub;
    Sub.cid = cid++;
    Sub.options = mergeOptions(
      Super.options,
      extendOptions
    );
    Sub['super'] = Super;

    // For props and computed properties, we define the proxy getters on
    // the Vue instances at extension time, on the extended prototype. This
    // avoids Object.defineProperty calls for each instance created.
    if (Sub.options.props) {
      initProps$1(Sub);
    }
    if (Sub.options.computed) {
      initComputed$1(Sub);
    }

    // allow further extension/mixin/plugin usage
    Sub.extend = Super.extend;
    Sub.mixin = Super.mixin;
    Sub.use = Super.use;

    // create asset registers, so extended classes
    // can have their private assets too.
    ASSET_TYPES.forEach(function (type) {
      Sub[type] = Super[type];
    });
    // enable recursive self-lookup
    if (name) {
      Sub.options.components[name] = Sub;
    }

    // keep a reference to the super options at extension time.
    // later at instantiation we can check if Super's options have
    // been updated.
    Sub.superOptions = Super.options;
    Sub.extendOptions = extendOptions;
    Sub.sealedOptions = extend({}, Sub.options);

    // cache constructor
    cachedCtors[SuperId] = Sub;
    return Sub
  };
}

function initProps$1 (Comp) {
  var props = Comp.options.props;
  for (var key in props) {
    proxy(Comp.prototype, "_props", key);
  }
}

function initComputed$1 (Comp) {
  var computed = Comp.options.computed;
  for (var key in computed) {
    defineComputed(Comp.prototype, key, computed[key]);
  }
}

/*  */

function initAssetRegisters (Vue) {
  /**
   * Create asset registration methods.
   */
  ASSET_TYPES.forEach(function (type) {
    Vue[type] = function (
      id,
      definition
    ) {
      if (!definition) {
        return this.options[type + 's'][id]
      } else {
        /* istanbul ignore if */
        if ( true && type === 'component') {
          validateComponentName(id);
        }
        if (type === 'component' && isPlainObject(definition)) {
          definition.name = definition.name || id;
          definition = this.options._base.extend(definition);
        }
        if (type === 'directive' && typeof definition === 'function') {
          definition = { bind: definition, update: definition };
        }
        this.options[type + 's'][id] = definition;
        return definition
      }
    };
  });
}

/*  */





function getComponentName (opts) {
  return opts && (opts.Ctor.options.name || opts.tag)
}

function matches (pattern, name) {
  if (Array.isArray(pattern)) {
    return pattern.indexOf(name) > -1
  } else if (typeof pattern === 'string') {
    return pattern.split(',').indexOf(name) > -1
  } else if (isRegExp(pattern)) {
    return pattern.test(name)
  }
  /* istanbul ignore next */
  return false
}

function pruneCache (keepAliveInstance, filter) {
  var cache = keepAliveInstance.cache;
  var keys = keepAliveInstance.keys;
  var _vnode = keepAliveInstance._vnode;
  for (var key in cache) {
    var entry = cache[key];
    if (entry) {
      var name = entry.name;
      if (name && !filter(name)) {
        pruneCacheEntry(cache, key, keys, _vnode);
      }
    }
  }
}

function pruneCacheEntry (
  cache,
  key,
  keys,
  current
) {
  var entry = cache[key];
  if (entry && (!current || entry.tag !== current.tag)) {
    entry.componentInstance.$destroy();
  }
  cache[key] = null;
  remove(keys, key);
}

var patternTypes = [String, RegExp, Array];

var KeepAlive = {
  name: 'keep-alive',
  abstract: true,

  props: {
    include: patternTypes,
    exclude: patternTypes,
    max: [String, Number]
  },

  methods: {
    cacheVNode: function cacheVNode() {
      var ref = this;
      var cache = ref.cache;
      var keys = ref.keys;
      var vnodeToCache = ref.vnodeToCache;
      var keyToCache = ref.keyToCache;
      if (vnodeToCache) {
        var tag = vnodeToCache.tag;
        var componentInstance = vnodeToCache.componentInstance;
        var componentOptions = vnodeToCache.componentOptions;
        cache[keyToCache] = {
          name: getComponentName(componentOptions),
          tag: tag,
          componentInstance: componentInstance,
        };
        keys.push(keyToCache);
        // prune oldest entry
        if (this.max && keys.length > parseInt(this.max)) {
          pruneCacheEntry(cache, keys[0], keys, this._vnode);
        }
        this.vnodeToCache = null;
      }
    }
  },

  created: function created () {
    this.cache = Object.create(null);
    this.keys = [];
  },

  destroyed: function destroyed () {
    for (var key in this.cache) {
      pruneCacheEntry(this.cache, key, this.keys);
    }
  },

  mounted: function mounted () {
    var this$1 = this;

    this.cacheVNode();
    this.$watch('include', function (val) {
      pruneCache(this$1, function (name) { return matches(val, name); });
    });
    this.$watch('exclude', function (val) {
      pruneCache(this$1, function (name) { return !matches(val, name); });
    });
  },

  updated: function updated () {
    this.cacheVNode();
  },

  render: function render () {
    var slot = this.$slots.default;
    var vnode = getFirstComponentChild(slot);
    var componentOptions = vnode && vnode.componentOptions;
    if (componentOptions) {
      // check pattern
      var name = getComponentName(componentOptions);
      var ref = this;
      var include = ref.include;
      var exclude = ref.exclude;
      if (
        // not included
        (include && (!name || !matches(include, name))) ||
        // excluded
        (exclude && name && matches(exclude, name))
      ) {
        return vnode
      }

      var ref$1 = this;
      var cache = ref$1.cache;
      var keys = ref$1.keys;
      var key = vnode.key == null
        // same constructor may get registered as different local components
        // so cid alone is not enough (#3269)
        ? componentOptions.Ctor.cid + (componentOptions.tag ? ("::" + (componentOptions.tag)) : '')
        : vnode.key;
      if (cache[key]) {
        vnode.componentInstance = cache[key].componentInstance;
        // make current key freshest
        remove(keys, key);
        keys.push(key);
      } else {
        // delay setting the cache until update
        this.vnodeToCache = vnode;
        this.keyToCache = key;
      }

      vnode.data.keepAlive = true;
    }
    return vnode || (slot && slot[0])
  }
};

var builtInComponents = {
  KeepAlive: KeepAlive
};

/*  */

function initGlobalAPI (Vue) {
  // config
  var configDef = {};
  configDef.get = function () { return config; };
  if (true) {
    configDef.set = function () {
      warn(
        'Do not replace the Vue.config object, set individual fields instead.'
      );
    };
  }
  Object.defineProperty(Vue, 'config', configDef);

  // exposed util methods.
  // NOTE: these are not considered part of the public API - avoid relying on
  // them unless you are aware of the risk.
  Vue.util = {
    warn: warn,
    extend: extend,
    mergeOptions: mergeOptions,
    defineReactive: defineReactive$$1
  };

  Vue.set = set;
  Vue.delete = del;
  Vue.nextTick = nextTick;

  // 2.6 explicit observable API
  Vue.observable = function (obj) {
    observe(obj);
    return obj
  };

  Vue.options = Object.create(null);
  ASSET_TYPES.forEach(function (type) {
    Vue.options[type + 's'] = Object.create(null);
  });

  // this is used to identify the "base" constructor to extend all plain-object
  // components with in Weex's multi-instance scenarios.
  Vue.options._base = Vue;

  extend(Vue.options.components, builtInComponents);

  initUse(Vue);
  initMixin$1(Vue);
  initExtend(Vue);
  initAssetRegisters(Vue);
}

initGlobalAPI(Vue);

Object.defineProperty(Vue.prototype, '$isServer', {
  get: isServerRendering
});

Object.defineProperty(Vue.prototype, '$ssrContext', {
  get: function get () {
    /* istanbul ignore next */
    return this.$vnode && this.$vnode.ssrContext
  }
});

// expose FunctionalRenderContext for ssr runtime helper installation
Object.defineProperty(Vue, 'FunctionalRenderContext', {
  value: FunctionalRenderContext
});

Vue.version = '2.6.14';

/*  */

// these are reserved for web because they are directly compiled away
// during template compilation
var isReservedAttr = makeMap('style,class');

// attributes that should be using props for binding
var acceptValue = makeMap('input,textarea,option,select,progress');
var mustUseProp = function (tag, type, attr) {
  return (
    (attr === 'value' && acceptValue(tag)) && type !== 'button' ||
    (attr === 'selected' && tag === 'option') ||
    (attr === 'checked' && tag === 'input') ||
    (attr === 'muted' && tag === 'video')
  )
};

var isEnumeratedAttr = makeMap('contenteditable,draggable,spellcheck');

var isValidContentEditableValue = makeMap('events,caret,typing,plaintext-only');

var convertEnumeratedValue = function (key, value) {
  return isFalsyAttrValue(value) || value === 'false'
    ? 'false'
    // allow arbitrary string value for contenteditable
    : key === 'contenteditable' && isValidContentEditableValue(value)
      ? value
      : 'true'
};

var isBooleanAttr = makeMap(
  'allowfullscreen,async,autofocus,autoplay,checked,compact,controls,declare,' +
  'default,defaultchecked,defaultmuted,defaultselected,defer,disabled,' +
  'enabled,formnovalidate,hidden,indeterminate,inert,ismap,itemscope,loop,multiple,' +
  'muted,nohref,noresize,noshade,novalidate,nowrap,open,pauseonexit,readonly,' +
  'required,reversed,scoped,seamless,selected,sortable,' +
  'truespeed,typemustmatch,visible'
);

var xlinkNS = 'http://www.w3.org/1999/xlink';

var isXlink = function (name) {
  return name.charAt(5) === ':' && name.slice(0, 5) === 'xlink'
};

var getXlinkProp = function (name) {
  return isXlink(name) ? name.slice(6, name.length) : ''
};

var isFalsyAttrValue = function (val) {
  return val == null || val === false
};

/*  */

function genClassForVnode (vnode) {
  var data = vnode.data;
  var parentNode = vnode;
  var childNode = vnode;
  while (isDef(childNode.componentInstance)) {
    childNode = childNode.componentInstance._vnode;
    if (childNode && childNode.data) {
      data = mergeClassData(childNode.data, data);
    }
  }
  while (isDef(parentNode = parentNode.parent)) {
    if (parentNode && parentNode.data) {
      data = mergeClassData(data, parentNode.data);
    }
  }
  return renderClass(data.staticClass, data.class)
}

function mergeClassData (child, parent) {
  return {
    staticClass: concat(child.staticClass, parent.staticClass),
    class: isDef(child.class)
      ? [child.class, parent.class]
      : parent.class
  }
}

function renderClass (
  staticClass,
  dynamicClass
) {
  if (isDef(staticClass) || isDef(dynamicClass)) {
    return concat(staticClass, stringifyClass(dynamicClass))
  }
  /* istanbul ignore next */
  return ''
}

function concat (a, b) {
  return a ? b ? (a + ' ' + b) : a : (b || '')
}

function stringifyClass (value) {
  if (Array.isArray(value)) {
    return stringifyArray(value)
  }
  if (isObject(value)) {
    return stringifyObject(value)
  }
  if (typeof value === 'string') {
    return value
  }
  /* istanbul ignore next */
  return ''
}

function stringifyArray (value) {
  var res = '';
  var stringified;
  for (var i = 0, l = value.length; i < l; i++) {
    if (isDef(stringified = stringifyClass(value[i])) && stringified !== '') {
      if (res) { res += ' '; }
      res += stringified;
    }
  }
  return res
}

function stringifyObject (value) {
  var res = '';
  for (var key in value) {
    if (value[key]) {
      if (res) { res += ' '; }
      res += key;
    }
  }
  return res
}

/*  */

var namespaceMap = {
  svg: 'http://www.w3.org/2000/svg',
  math: 'http://www.w3.org/1998/Math/MathML'
};

var isHTMLTag = makeMap(
  'html,body,base,head,link,meta,style,title,' +
  'address,article,aside,footer,header,h1,h2,h3,h4,h5,h6,hgroup,nav,section,' +
  'div,dd,dl,dt,figcaption,figure,picture,hr,img,li,main,ol,p,pre,ul,' +
  'a,b,abbr,bdi,bdo,br,cite,code,data,dfn,em,i,kbd,mark,q,rp,rt,rtc,ruby,' +
  's,samp,small,span,strong,sub,sup,time,u,var,wbr,area,audio,map,track,video,' +
  'embed,object,param,source,canvas,script,noscript,del,ins,' +
  'caption,col,colgroup,table,thead,tbody,td,th,tr,' +
  'button,datalist,fieldset,form,input,label,legend,meter,optgroup,option,' +
  'output,progress,select,textarea,' +
  'details,dialog,menu,menuitem,summary,' +
  'content,element,shadow,template,blockquote,iframe,tfoot'
);

// this map is intentionally selective, only covering SVG elements that may
// contain child elements.
var isSVG = makeMap(
  'svg,animate,circle,clippath,cursor,defs,desc,ellipse,filter,font-face,' +
  'foreignobject,g,glyph,image,line,marker,mask,missing-glyph,path,pattern,' +
  'polygon,polyline,rect,switch,symbol,text,textpath,tspan,use,view',
  true
);

var isReservedTag = function (tag) {
  return isHTMLTag(tag) || isSVG(tag)
};

function getTagNamespace (tag) {
  if (isSVG(tag)) {
    return 'svg'
  }
  // basic support for MathML
  // note it doesn't support other MathML elements being component roots
  if (tag === 'math') {
    return 'math'
  }
}

var unknownElementCache = Object.create(null);
function isUnknownElement (tag) {
  /* istanbul ignore if */
  if (!inBrowser) {
    return true
  }
  if (isReservedTag(tag)) {
    return false
  }
  tag = tag.toLowerCase();
  /* istanbul ignore if */
  if (unknownElementCache[tag] != null) {
    return unknownElementCache[tag]
  }
  var el = document.createElement(tag);
  if (tag.indexOf('-') > -1) {
    // http://stackoverflow.com/a/28210364/1070244
    return (unknownElementCache[tag] = (
      el.constructor === window.HTMLUnknownElement ||
      el.constructor === window.HTMLElement
    ))
  } else {
    return (unknownElementCache[tag] = /HTMLUnknownElement/.test(el.toString()))
  }
}

var isTextInputType = makeMap('text,number,password,search,email,tel,url');

/*  */

/**
 * Query an element selector if it's not an element already.
 */
function query (el) {
  if (typeof el === 'string') {
    var selected = document.querySelector(el);
    if (!selected) {
       true && warn(
        'Cannot find element: ' + el
      );
      return document.createElement('div')
    }
    return selected
  } else {
    return el
  }
}

/*  */

function createElement$1 (tagName, vnode) {
  var elm = document.createElement(tagName);
  if (tagName !== 'select') {
    return elm
  }
  // false or null will remove the attribute but undefined will not
  if (vnode.data && vnode.data.attrs && vnode.data.attrs.multiple !== undefined) {
    elm.setAttribute('multiple', 'multiple');
  }
  return elm
}

function createElementNS (namespace, tagName) {
  return document.createElementNS(namespaceMap[namespace], tagName)
}

function createTextNode (text) {
  return document.createTextNode(text)
}

function createComment (text) {
  return document.createComment(text)
}

function insertBefore (parentNode, newNode, referenceNode) {
  parentNode.insertBefore(newNode, referenceNode);
}

function removeChild (node, child) {
  node.removeChild(child);
}

function appendChild (node, child) {
  node.appendChild(child);
}

function parentNode (node) {
  return node.parentNode
}

function nextSibling (node) {
  return node.nextSibling
}

function tagName (node) {
  return node.tagName
}

function setTextContent (node, text) {
  node.textContent = text;
}

function setStyleScope (node, scopeId) {
  node.setAttribute(scopeId, '');
}

var nodeOps = /*#__PURE__*/Object.freeze({
  createElement: createElement$1,
  createElementNS: createElementNS,
  createTextNode: createTextNode,
  createComment: createComment,
  insertBefore: insertBefore,
  removeChild: removeChild,
  appendChild: appendChild,
  parentNode: parentNode,
  nextSibling: nextSibling,
  tagName: tagName,
  setTextContent: setTextContent,
  setStyleScope: setStyleScope
});

/*  */

var ref = {
  create: function create (_, vnode) {
    registerRef(vnode);
  },
  update: function update (oldVnode, vnode) {
    if (oldVnode.data.ref !== vnode.data.ref) {
      registerRef(oldVnode, true);
      registerRef(vnode);
    }
  },
  destroy: function destroy (vnode) {
    registerRef(vnode, true);
  }
};

function registerRef (vnode, isRemoval) {
  var key = vnode.data.ref;
  if (!isDef(key)) { return }

  var vm = vnode.context;
  var ref = vnode.componentInstance || vnode.elm;
  var refs = vm.$refs;
  if (isRemoval) {
    if (Array.isArray(refs[key])) {
      remove(refs[key], ref);
    } else if (refs[key] === ref) {
      refs[key] = undefined;
    }
  } else {
    if (vnode.data.refInFor) {
      if (!Array.isArray(refs[key])) {
        refs[key] = [ref];
      } else if (refs[key].indexOf(ref) < 0) {
        // $flow-disable-line
        refs[key].push(ref);
      }
    } else {
      refs[key] = ref;
    }
  }
}

/**
 * Virtual DOM patching algorithm based on Snabbdom by
 * Simon Friis Vindum (@paldepind)
 * Licensed under the MIT License
 * https://github.com/paldepind/snabbdom/blob/master/LICENSE
 *
 * modified by Evan You (@yyx990803)
 *
 * Not type-checking this because this file is perf-critical and the cost
 * of making flow understand it is not worth it.
 */

var emptyNode = new VNode('', {}, []);

var hooks = ['create', 'activate', 'update', 'remove', 'destroy'];

function sameVnode (a, b) {
  return (
    a.key === b.key &&
    a.asyncFactory === b.asyncFactory && (
      (
        a.tag === b.tag &&
        a.isComment === b.isComment &&
        isDef(a.data) === isDef(b.data) &&
        sameInputType(a, b)
      ) || (
        isTrue(a.isAsyncPlaceholder) &&
        isUndef(b.asyncFactory.error)
      )
    )
  )
}

function sameInputType (a, b) {
  if (a.tag !== 'input') { return true }
  var i;
  var typeA = isDef(i = a.data) && isDef(i = i.attrs) && i.type;
  var typeB = isDef(i = b.data) && isDef(i = i.attrs) && i.type;
  return typeA === typeB || isTextInputType(typeA) && isTextInputType(typeB)
}

function createKeyToOldIdx (children, beginIdx, endIdx) {
  var i, key;
  var map = {};
  for (i = beginIdx; i <= endIdx; ++i) {
    key = children[i].key;
    if (isDef(key)) { map[key] = i; }
  }
  return map
}

function createPatchFunction (backend) {
  var i, j;
  var cbs = {};

  var modules = backend.modules;
  var nodeOps = backend.nodeOps;

  for (i = 0; i < hooks.length; ++i) {
    cbs[hooks[i]] = [];
    for (j = 0; j < modules.length; ++j) {
      if (isDef(modules[j][hooks[i]])) {
        cbs[hooks[i]].push(modules[j][hooks[i]]);
      }
    }
  }

  function emptyNodeAt (elm) {
    return new VNode(nodeOps.tagName(elm).toLowerCase(), {}, [], undefined, elm)
  }

  function createRmCb (childElm, listeners) {
    function remove$$1 () {
      if (--remove$$1.listeners === 0) {
        removeNode(childElm);
      }
    }
    remove$$1.listeners = listeners;
    return remove$$1
  }

  function removeNode (el) {
    var parent = nodeOps.parentNode(el);
    // element may have already been removed due to v-html / v-text
    if (isDef(parent)) {
      nodeOps.removeChild(parent, el);
    }
  }

  function isUnknownElement$$1 (vnode, inVPre) {
    return (
      !inVPre &&
      !vnode.ns &&
      !(
        config.ignoredElements.length &&
        config.ignoredElements.some(function (ignore) {
          return isRegExp(ignore)
            ? ignore.test(vnode.tag)
            : ignore === vnode.tag
        })
      ) &&
      config.isUnknownElement(vnode.tag)
    )
  }

  var creatingElmInVPre = 0;

  function createElm (
    vnode,
    insertedVnodeQueue,
    parentElm,
    refElm,
    nested,
    ownerArray,
    index
  ) {
    if (isDef(vnode.elm) && isDef(ownerArray)) {
      // This vnode was used in a previous render!
      // now it's used as a new node, overwriting its elm would cause
      // potential patch errors down the road when it's used as an insertion
      // reference node. Instead, we clone the node on-demand before creating
      // associated DOM element for it.
      vnode = ownerArray[index] = cloneVNode(vnode);
    }

    vnode.isRootInsert = !nested; // for transition enter check
    if (createComponent(vnode, insertedVnodeQueue, parentElm, refElm)) {
      return
    }

    var data = vnode.data;
    var children = vnode.children;
    var tag = vnode.tag;
    if (isDef(tag)) {
      if (true) {
        if (data && data.pre) {
          creatingElmInVPre++;
        }
        if (isUnknownElement$$1(vnode, creatingElmInVPre)) {
          warn(
            'Unknown custom element: <' + tag + '> - did you ' +
            'register the component correctly? For recursive components, ' +
            'make sure to provide the "name" option.',
            vnode.context
          );
        }
      }

      vnode.elm = vnode.ns
        ? nodeOps.createElementNS(vnode.ns, tag)
        : nodeOps.createElement(tag, vnode);
      setScope(vnode);

      /* istanbul ignore if */
      {
        createChildren(vnode, children, insertedVnodeQueue);
        if (isDef(data)) {
          invokeCreateHooks(vnode, insertedVnodeQueue);
        }
        insert(parentElm, vnode.elm, refElm);
      }

      if ( true && data && data.pre) {
        creatingElmInVPre--;
      }
    } else if (isTrue(vnode.isComment)) {
      vnode.elm = nodeOps.createComment(vnode.text);
      insert(parentElm, vnode.elm, refElm);
    } else {
      vnode.elm = nodeOps.createTextNode(vnode.text);
      insert(parentElm, vnode.elm, refElm);
    }
  }

  function createComponent (vnode, insertedVnodeQueue, parentElm, refElm) {
    var i = vnode.data;
    if (isDef(i)) {
      var isReactivated = isDef(vnode.componentInstance) && i.keepAlive;
      if (isDef(i = i.hook) && isDef(i = i.init)) {
        i(vnode, false /* hydrating */);
      }
      // after calling the init hook, if the vnode is a child component
      // it should've created a child instance and mounted it. the child
      // component also has set the placeholder vnode's elm.
      // in that case we can just return the element and be done.
      if (isDef(vnode.componentInstance)) {
        initComponent(vnode, insertedVnodeQueue);
        insert(parentElm, vnode.elm, refElm);
        if (isTrue(isReactivated)) {
          reactivateComponent(vnode, insertedVnodeQueue, parentElm, refElm);
        }
        return true
      }
    }
  }

  function initComponent (vnode, insertedVnodeQueue) {
    if (isDef(vnode.data.pendingInsert)) {
      insertedVnodeQueue.push.apply(insertedVnodeQueue, vnode.data.pendingInsert);
      vnode.data.pendingInsert = null;
    }
    vnode.elm = vnode.componentInstance.$el;
    if (isPatchable(vnode)) {
      invokeCreateHooks(vnode, insertedVnodeQueue);
      setScope(vnode);
    } else {
      // empty component root.
      // skip all element-related modules except for ref (#3455)
      registerRef(vnode);
      // make sure to invoke the insert hook
      insertedVnodeQueue.push(vnode);
    }
  }

  function reactivateComponent (vnode, insertedVnodeQueue, parentElm, refElm) {
    var i;
    // hack for #4339: a reactivated component with inner transition
    // does not trigger because the inner node's created hooks are not called
    // again. It's not ideal to involve module-specific logic in here but
    // there doesn't seem to be a better way to do it.
    var innerNode = vnode;
    while (innerNode.componentInstance) {
      innerNode = innerNode.componentInstance._vnode;
      if (isDef(i = innerNode.data) && isDef(i = i.transition)) {
        for (i = 0; i < cbs.activate.length; ++i) {
          cbs.activate[i](emptyNode, innerNode);
        }
        insertedVnodeQueue.push(innerNode);
        break
      }
    }
    // unlike a newly created component,
    // a reactivated keep-alive component doesn't insert itself
    insert(parentElm, vnode.elm, refElm);
  }

  function insert (parent, elm, ref$$1) {
    if (isDef(parent)) {
      if (isDef(ref$$1)) {
        if (nodeOps.parentNode(ref$$1) === parent) {
          nodeOps.insertBefore(parent, elm, ref$$1);
        }
      } else {
        nodeOps.appendChild(parent, elm);
      }
    }
  }

  function createChildren (vnode, children, insertedVnodeQueue) {
    if (Array.isArray(children)) {
      if (true) {
        checkDuplicateKeys(children);
      }
      for (var i = 0; i < children.length; ++i) {
        createElm(children[i], insertedVnodeQueue, vnode.elm, null, true, children, i);
      }
    } else if (isPrimitive(vnode.text)) {
      nodeOps.appendChild(vnode.elm, nodeOps.createTextNode(String(vnode.text)));
    }
  }

  function isPatchable (vnode) {
    while (vnode.componentInstance) {
      vnode = vnode.componentInstance._vnode;
    }
    return isDef(vnode.tag)
  }

  function invokeCreateHooks (vnode, insertedVnodeQueue) {
    for (var i$1 = 0; i$1 < cbs.create.length; ++i$1) {
      cbs.create[i$1](emptyNode, vnode);
    }
    i = vnode.data.hook; // Reuse variable
    if (isDef(i)) {
      if (isDef(i.create)) { i.create(emptyNode, vnode); }
      if (isDef(i.insert)) { insertedVnodeQueue.push(vnode); }
    }
  }

  // set scope id attribute for scoped CSS.
  // this is implemented as a special case to avoid the overhead
  // of going through the normal attribute patching process.
  function setScope (vnode) {
    var i;
    if (isDef(i = vnode.fnScopeId)) {
      nodeOps.setStyleScope(vnode.elm, i);
    } else {
      var ancestor = vnode;
      while (ancestor) {
        if (isDef(i = ancestor.context) && isDef(i = i.$options._scopeId)) {
          nodeOps.setStyleScope(vnode.elm, i);
        }
        ancestor = ancestor.parent;
      }
    }
    // for slot content they should also get the scopeId from the host instance.
    if (isDef(i = activeInstance) &&
      i !== vnode.context &&
      i !== vnode.fnContext &&
      isDef(i = i.$options._scopeId)
    ) {
      nodeOps.setStyleScope(vnode.elm, i);
    }
  }

  function addVnodes (parentElm, refElm, vnodes, startIdx, endIdx, insertedVnodeQueue) {
    for (; startIdx <= endIdx; ++startIdx) {
      createElm(vnodes[startIdx], insertedVnodeQueue, parentElm, refElm, false, vnodes, startIdx);
    }
  }

  function invokeDestroyHook (vnode) {
    var i, j;
    var data = vnode.data;
    if (isDef(data)) {
      if (isDef(i = data.hook) && isDef(i = i.destroy)) { i(vnode); }
      for (i = 0; i < cbs.destroy.length; ++i) { cbs.destroy[i](vnode); }
    }
    if (isDef(i = vnode.children)) {
      for (j = 0; j < vnode.children.length; ++j) {
        invokeDestroyHook(vnode.children[j]);
      }
    }
  }

  function removeVnodes (vnodes, startIdx, endIdx) {
    for (; startIdx <= endIdx; ++startIdx) {
      var ch = vnodes[startIdx];
      if (isDef(ch)) {
        if (isDef(ch.tag)) {
          removeAndInvokeRemoveHook(ch);
          invokeDestroyHook(ch);
        } else { // Text node
          removeNode(ch.elm);
        }
      }
    }
  }

  function removeAndInvokeRemoveHook (vnode, rm) {
    if (isDef(rm) || isDef(vnode.data)) {
      var i;
      var listeners = cbs.remove.length + 1;
      if (isDef(rm)) {
        // we have a recursively passed down rm callback
        // increase the listeners count
        rm.listeners += listeners;
      } else {
        // directly removing
        rm = createRmCb(vnode.elm, listeners);
      }
      // recursively invoke hooks on child component root node
      if (isDef(i = vnode.componentInstance) && isDef(i = i._vnode) && isDef(i.data)) {
        removeAndInvokeRemoveHook(i, rm);
      }
      for (i = 0; i < cbs.remove.length; ++i) {
        cbs.remove[i](vnode, rm);
      }
      if (isDef(i = vnode.data.hook) && isDef(i = i.remove)) {
        i(vnode, rm);
      } else {
        rm();
      }
    } else {
      removeNode(vnode.elm);
    }
  }

  function updateChildren (parentElm, oldCh, newCh, insertedVnodeQueue, removeOnly) {
    var oldStartIdx = 0;
    var newStartIdx = 0;
    var oldEndIdx = oldCh.length - 1;
    var oldStartVnode = oldCh[0];
    var oldEndVnode = oldCh[oldEndIdx];
    var newEndIdx = newCh.length - 1;
    var newStartVnode = newCh[0];
    var newEndVnode = newCh[newEndIdx];
    var oldKeyToIdx, idxInOld, vnodeToMove, refElm;

    // removeOnly is a special flag used only by <transition-group>
    // to ensure removed elements stay in correct relative positions
    // during leaving transitions
    var canMove = !removeOnly;

    if (true) {
      checkDuplicateKeys(newCh);
    }

    while (oldStartIdx <= oldEndIdx && newStartIdx <= newEndIdx) {
      if (isUndef(oldStartVnode)) {
        oldStartVnode = oldCh[++oldStartIdx]; // Vnode has been moved left
      } else if (isUndef(oldEndVnode)) {
        oldEndVnode = oldCh[--oldEndIdx];
      } else if (sameVnode(oldStartVnode, newStartVnode)) {
        patchVnode(oldStartVnode, newStartVnode, insertedVnodeQueue, newCh, newStartIdx);
        oldStartVnode = oldCh[++oldStartIdx];
        newStartVnode = newCh[++newStartIdx];
      } else if (sameVnode(oldEndVnode, newEndVnode)) {
        patchVnode(oldEndVnode, newEndVnode, insertedVnodeQueue, newCh, newEndIdx);
        oldEndVnode = oldCh[--oldEndIdx];
        newEndVnode = newCh[--newEndIdx];
      } else if (sameVnode(oldStartVnode, newEndVnode)) { // Vnode moved right
        patchVnode(oldStartVnode, newEndVnode, insertedVnodeQueue, newCh, newEndIdx);
        canMove && nodeOps.insertBefore(parentElm, oldStartVnode.elm, nodeOps.nextSibling(oldEndVnode.elm));
        oldStartVnode = oldCh[++oldStartIdx];
        newEndVnode = newCh[--newEndIdx];
      } else if (sameVnode(oldEndVnode, newStartVnode)) { // Vnode moved left
        patchVnode(oldEndVnode, newStartVnode, insertedVnodeQueue, newCh, newStartIdx);
        canMove && nodeOps.insertBefore(parentElm, oldEndVnode.elm, oldStartVnode.elm);
        oldEndVnode = oldCh[--oldEndIdx];
        newStartVnode = newCh[++newStartIdx];
      } else {
        if (isUndef(oldKeyToIdx)) { oldKeyToIdx = createKeyToOldIdx(oldCh, oldStartIdx, oldEndIdx); }
        idxInOld = isDef(newStartVnode.key)
          ? oldKeyToIdx[newStartVnode.key]
          : findIdxInOld(newStartVnode, oldCh, oldStartIdx, oldEndIdx);
        if (isUndef(idxInOld)) { // New element
          createElm(newStartVnode, insertedVnodeQueue, parentElm, oldStartVnode.elm, false, newCh, newStartIdx);
        } else {
          vnodeToMove = oldCh[idxInOld];
          if (sameVnode(vnodeToMove, newStartVnode)) {
            patchVnode(vnodeToMove, newStartVnode, insertedVnodeQueue, newCh, newStartIdx);
            oldCh[idxInOld] = undefined;
            canMove && nodeOps.insertBefore(parentElm, vnodeToMove.elm, oldStartVnode.elm);
          } else {
            // same key but different element. treat as new element
            createElm(newStartVnode, insertedVnodeQueue, parentElm, oldStartVnode.elm, false, newCh, newStartIdx);
          }
        }
        newStartVnode = newCh[++newStartIdx];
      }
    }
    if (oldStartIdx > oldEndIdx) {
      refElm = isUndef(newCh[newEndIdx + 1]) ? null : newCh[newEndIdx + 1].elm;
      addVnodes(parentElm, refElm, newCh, newStartIdx, newEndIdx, insertedVnodeQueue);
    } else if (newStartIdx > newEndIdx) {
      removeVnodes(oldCh, oldStartIdx, oldEndIdx);
    }
  }

  function checkDuplicateKeys (children) {
    var seenKeys = {};
    for (var i = 0; i < children.length; i++) {
      var vnode = children[i];
      var key = vnode.key;
      if (isDef(key)) {
        if (seenKeys[key]) {
          warn(
            ("Duplicate keys detected: '" + key + "'. This may cause an update error."),
            vnode.context
          );
        } else {
          seenKeys[key] = true;
        }
      }
    }
  }

  function findIdxInOld (node, oldCh, start, end) {
    for (var i = start; i < end; i++) {
      var c = oldCh[i];
      if (isDef(c) && sameVnode(node, c)) { return i }
    }
  }

  function patchVnode (
    oldVnode,
    vnode,
    insertedVnodeQueue,
    ownerArray,
    index,
    removeOnly
  ) {
    if (oldVnode === vnode) {
      return
    }

    if (isDef(vnode.elm) && isDef(ownerArray)) {
      // clone reused vnode
      vnode = ownerArray[index] = cloneVNode(vnode);
    }

    var elm = vnode.elm = oldVnode.elm;

    if (isTrue(oldVnode.isAsyncPlaceholder)) {
      if (isDef(vnode.asyncFactory.resolved)) {
        hydrate(oldVnode.elm, vnode, insertedVnodeQueue);
      } else {
        vnode.isAsyncPlaceholder = true;
      }
      return
    }

    // reuse element for static trees.
    // note we only do this if the vnode is cloned -
    // if the new node is not cloned it means the render functions have been
    // reset by the hot-reload-api and we need to do a proper re-render.
    if (isTrue(vnode.isStatic) &&
      isTrue(oldVnode.isStatic) &&
      vnode.key === oldVnode.key &&
      (isTrue(vnode.isCloned) || isTrue(vnode.isOnce))
    ) {
      vnode.componentInstance = oldVnode.componentInstance;
      return
    }

    var i;
    var data = vnode.data;
    if (isDef(data) && isDef(i = data.hook) && isDef(i = i.prepatch)) {
      i(oldVnode, vnode);
    }

    var oldCh = oldVnode.children;
    var ch = vnode.children;
    if (isDef(data) && isPatchable(vnode)) {
      for (i = 0; i < cbs.update.length; ++i) { cbs.update[i](oldVnode, vnode); }
      if (isDef(i = data.hook) && isDef(i = i.update)) { i(oldVnode, vnode); }
    }
    if (isUndef(vnode.text)) {
      if (isDef(oldCh) && isDef(ch)) {
        if (oldCh !== ch) { updateChildren(elm, oldCh, ch, insertedVnodeQueue, removeOnly); }
      } else if (isDef(ch)) {
        if (true) {
          checkDuplicateKeys(ch);
        }
        if (isDef(oldVnode.text)) { nodeOps.setTextContent(elm, ''); }
        addVnodes(elm, null, ch, 0, ch.length - 1, insertedVnodeQueue);
      } else if (isDef(oldCh)) {
        removeVnodes(oldCh, 0, oldCh.length - 1);
      } else if (isDef(oldVnode.text)) {
        nodeOps.setTextContent(elm, '');
      }
    } else if (oldVnode.text !== vnode.text) {
      nodeOps.setTextContent(elm, vnode.text);
    }
    if (isDef(data)) {
      if (isDef(i = data.hook) && isDef(i = i.postpatch)) { i(oldVnode, vnode); }
    }
  }

  function invokeInsertHook (vnode, queue, initial) {
    // delay insert hooks for component root nodes, invoke them after the
    // element is really inserted
    if (isTrue(initial) && isDef(vnode.parent)) {
      vnode.parent.data.pendingInsert = queue;
    } else {
      for (var i = 0; i < queue.length; ++i) {
        queue[i].data.hook.insert(queue[i]);
      }
    }
  }

  var hydrationBailed = false;
  // list of modules that can skip create hook during hydration because they
  // are already rendered on the client or has no need for initialization
  // Note: style is excluded because it relies on initial clone for future
  // deep updates (#7063).
  var isRenderedModule = makeMap('attrs,class,staticClass,staticStyle,key');

  // Note: this is a browser-only function so we can assume elms are DOM nodes.
  function hydrate (elm, vnode, insertedVnodeQueue, inVPre) {
    var i;
    var tag = vnode.tag;
    var data = vnode.data;
    var children = vnode.children;
    inVPre = inVPre || (data && data.pre);
    vnode.elm = elm;

    if (isTrue(vnode.isComment) && isDef(vnode.asyncFactory)) {
      vnode.isAsyncPlaceholder = true;
      return true
    }
    // assert node match
    if (true) {
      if (!assertNodeMatch(elm, vnode, inVPre)) {
        return false
      }
    }
    if (isDef(data)) {
      if (isDef(i = data.hook) && isDef(i = i.init)) { i(vnode, true /* hydrating */); }
      if (isDef(i = vnode.componentInstance)) {
        // child component. it should have hydrated its own tree.
        initComponent(vnode, insertedVnodeQueue);
        return true
      }
    }
    if (isDef(tag)) {
      if (isDef(children)) {
        // empty element, allow client to pick up and populate children
        if (!elm.hasChildNodes()) {
          createChildren(vnode, children, insertedVnodeQueue);
        } else {
          // v-html and domProps: innerHTML
          if (isDef(i = data) && isDef(i = i.domProps) && isDef(i = i.innerHTML)) {
            if (i !== elm.innerHTML) {
              /* istanbul ignore if */
              if ( true &&
                typeof console !== 'undefined' &&
                !hydrationBailed
              ) {
                hydrationBailed = true;
                console.warn('Parent: ', elm);
                console.warn('server innerHTML: ', i);
                console.warn('client innerHTML: ', elm.innerHTML);
              }
              return false
            }
          } else {
            // iterate and compare children lists
            var childrenMatch = true;
            var childNode = elm.firstChild;
            for (var i$1 = 0; i$1 < children.length; i$1++) {
              if (!childNode || !hydrate(childNode, children[i$1], insertedVnodeQueue, inVPre)) {
                childrenMatch = false;
                break
              }
              childNode = childNode.nextSibling;
            }
            // if childNode is not null, it means the actual childNodes list is
            // longer than the virtual children list.
            if (!childrenMatch || childNode) {
              /* istanbul ignore if */
              if ( true &&
                typeof console !== 'undefined' &&
                !hydrationBailed
              ) {
                hydrationBailed = true;
                console.warn('Parent: ', elm);
                console.warn('Mismatching childNodes vs. VNodes: ', elm.childNodes, children);
              }
              return false
            }
          }
        }
      }
      if (isDef(data)) {
        var fullInvoke = false;
        for (var key in data) {
          if (!isRenderedModule(key)) {
            fullInvoke = true;
            invokeCreateHooks(vnode, insertedVnodeQueue);
            break
          }
        }
        if (!fullInvoke && data['class']) {
          // ensure collecting deps for deep class bindings for future updates
          traverse(data['class']);
        }
      }
    } else if (elm.data !== vnode.text) {
      elm.data = vnode.text;
    }
    return true
  }

  function assertNodeMatch (node, vnode, inVPre) {
    if (isDef(vnode.tag)) {
      return vnode.tag.indexOf('vue-component') === 0 || (
        !isUnknownElement$$1(vnode, inVPre) &&
        vnode.tag.toLowerCase() === (node.tagName && node.tagName.toLowerCase())
      )
    } else {
      return node.nodeType === (vnode.isComment ? 8 : 3)
    }
  }

  return function patch (oldVnode, vnode, hydrating, removeOnly) {
    if (isUndef(vnode)) {
      if (isDef(oldVnode)) { invokeDestroyHook(oldVnode); }
      return
    }

    var isInitialPatch = false;
    var insertedVnodeQueue = [];

    if (isUndef(oldVnode)) {
      // empty mount (likely as component), create new root element
      isInitialPatch = true;
      createElm(vnode, insertedVnodeQueue);
    } else {
      var isRealElement = isDef(oldVnode.nodeType);
      if (!isRealElement && sameVnode(oldVnode, vnode)) {
        // patch existing root node
        patchVnode(oldVnode, vnode, insertedVnodeQueue, null, null, removeOnly);
      } else {
        if (isRealElement) {
          // mounting to a real element
          // check if this is server-rendered content and if we can perform
          // a successful hydration.
          if (oldVnode.nodeType === 1 && oldVnode.hasAttribute(SSR_ATTR)) {
            oldVnode.removeAttribute(SSR_ATTR);
            hydrating = true;
          }
          if (isTrue(hydrating)) {
            if (hydrate(oldVnode, vnode, insertedVnodeQueue)) {
              invokeInsertHook(vnode, insertedVnodeQueue, true);
              return oldVnode
            } else if (true) {
              warn(
                'The client-side rendered virtual DOM tree is not matching ' +
                'server-rendered content. This is likely caused by incorrect ' +
                'HTML markup, for example nesting block-level elements inside ' +
                '<p>, or missing <tbody>. Bailing hydration and performing ' +
                'full client-side render.'
              );
            }
          }
          // either not server-rendered, or hydration failed.
          // create an empty node and replace it
          oldVnode = emptyNodeAt(oldVnode);
        }

        // replacing existing element
        var oldElm = oldVnode.elm;
        var parentElm = nodeOps.parentNode(oldElm);

        // create new node
        createElm(
          vnode,
          insertedVnodeQueue,
          // extremely rare edge case: do not insert if old element is in a
          // leaving transition. Only happens when combining transition +
          // keep-alive + HOCs. (#4590)
          oldElm._leaveCb ? null : parentElm,
          nodeOps.nextSibling(oldElm)
        );

        // update parent placeholder node element, recursively
        if (isDef(vnode.parent)) {
          var ancestor = vnode.parent;
          var patchable = isPatchable(vnode);
          while (ancestor) {
            for (var i = 0; i < cbs.destroy.length; ++i) {
              cbs.destroy[i](ancestor);
            }
            ancestor.elm = vnode.elm;
            if (patchable) {
              for (var i$1 = 0; i$1 < cbs.create.length; ++i$1) {
                cbs.create[i$1](emptyNode, ancestor);
              }
              // #6513
              // invoke insert hooks that may have been merged by create hooks.
              // e.g. for directives that uses the "inserted" hook.
              var insert = ancestor.data.hook.insert;
              if (insert.merged) {
                // start at index 1 to avoid re-invoking component mounted hook
                for (var i$2 = 1; i$2 < insert.fns.length; i$2++) {
                  insert.fns[i$2]();
                }
              }
            } else {
              registerRef(ancestor);
            }
            ancestor = ancestor.parent;
          }
        }

        // destroy old node
        if (isDef(parentElm)) {
          removeVnodes([oldVnode], 0, 0);
        } else if (isDef(oldVnode.tag)) {
          invokeDestroyHook(oldVnode);
        }
      }
    }

    invokeInsertHook(vnode, insertedVnodeQueue, isInitialPatch);
    return vnode.elm
  }
}

/*  */

var directives = {
  create: updateDirectives,
  update: updateDirectives,
  destroy: function unbindDirectives (vnode) {
    updateDirectives(vnode, emptyNode);
  }
};

function updateDirectives (oldVnode, vnode) {
  if (oldVnode.data.directives || vnode.data.directives) {
    _update(oldVnode, vnode);
  }
}

function _update (oldVnode, vnode) {
  var isCreate = oldVnode === emptyNode;
  var isDestroy = vnode === emptyNode;
  var oldDirs = normalizeDirectives$1(oldVnode.data.directives, oldVnode.context);
  var newDirs = normalizeDirectives$1(vnode.data.directives, vnode.context);

  var dirsWithInsert = [];
  var dirsWithPostpatch = [];

  var key, oldDir, dir;
  for (key in newDirs) {
    oldDir = oldDirs[key];
    dir = newDirs[key];
    if (!oldDir) {
      // new directive, bind
      callHook$1(dir, 'bind', vnode, oldVnode);
      if (dir.def && dir.def.inserted) {
        dirsWithInsert.push(dir);
      }
    } else {
      // existing directive, update
      dir.oldValue = oldDir.value;
      dir.oldArg = oldDir.arg;
      callHook$1(dir, 'update', vnode, oldVnode);
      if (dir.def && dir.def.componentUpdated) {
        dirsWithPostpatch.push(dir);
      }
    }
  }

  if (dirsWithInsert.length) {
    var callInsert = function () {
      for (var i = 0; i < dirsWithInsert.length; i++) {
        callHook$1(dirsWithInsert[i], 'inserted', vnode, oldVnode);
      }
    };
    if (isCreate) {
      mergeVNodeHook(vnode, 'insert', callInsert);
    } else {
      callInsert();
    }
  }

  if (dirsWithPostpatch.length) {
    mergeVNodeHook(vnode, 'postpatch', function () {
      for (var i = 0; i < dirsWithPostpatch.length; i++) {
        callHook$1(dirsWithPostpatch[i], 'componentUpdated', vnode, oldVnode);
      }
    });
  }

  if (!isCreate) {
    for (key in oldDirs) {
      if (!newDirs[key]) {
        // no longer present, unbind
        callHook$1(oldDirs[key], 'unbind', oldVnode, oldVnode, isDestroy);
      }
    }
  }
}

var emptyModifiers = Object.create(null);

function normalizeDirectives$1 (
  dirs,
  vm
) {
  var res = Object.create(null);
  if (!dirs) {
    // $flow-disable-line
    return res
  }
  var i, dir;
  for (i = 0; i < dirs.length; i++) {
    dir = dirs[i];
    if (!dir.modifiers) {
      // $flow-disable-line
      dir.modifiers = emptyModifiers;
    }
    res[getRawDirName(dir)] = dir;
    dir.def = resolveAsset(vm.$options, 'directives', dir.name, true);
  }
  // $flow-disable-line
  return res
}

function getRawDirName (dir) {
  return dir.rawName || ((dir.name) + "." + (Object.keys(dir.modifiers || {}).join('.')))
}

function callHook$1 (dir, hook, vnode, oldVnode, isDestroy) {
  var fn = dir.def && dir.def[hook];
  if (fn) {
    try {
      fn(vnode.elm, dir, vnode, oldVnode, isDestroy);
    } catch (e) {
      handleError(e, vnode.context, ("directive " + (dir.name) + " " + hook + " hook"));
    }
  }
}

var baseModules = [
  ref,
  directives
];

/*  */

function updateAttrs (oldVnode, vnode) {
  var opts = vnode.componentOptions;
  if (isDef(opts) && opts.Ctor.options.inheritAttrs === false) {
    return
  }
  if (isUndef(oldVnode.data.attrs) && isUndef(vnode.data.attrs)) {
    return
  }
  var key, cur, old;
  var elm = vnode.elm;
  var oldAttrs = oldVnode.data.attrs || {};
  var attrs = vnode.data.attrs || {};
  // clone observed objects, as the user probably wants to mutate it
  if (isDef(attrs.__ob__)) {
    attrs = vnode.data.attrs = extend({}, attrs);
  }

  for (key in attrs) {
    cur = attrs[key];
    old = oldAttrs[key];
    if (old !== cur) {
      setAttr(elm, key, cur, vnode.data.pre);
    }
  }
  // #4391: in IE9, setting type can reset value for input[type=radio]
  // #6666: IE/Edge forces progress value down to 1 before setting a max
  /* istanbul ignore if */
  if ((isIE || isEdge) && attrs.value !== oldAttrs.value) {
    setAttr(elm, 'value', attrs.value);
  }
  for (key in oldAttrs) {
    if (isUndef(attrs[key])) {
      if (isXlink(key)) {
        elm.removeAttributeNS(xlinkNS, getXlinkProp(key));
      } else if (!isEnumeratedAttr(key)) {
        elm.removeAttribute(key);
      }
    }
  }
}

function setAttr (el, key, value, isInPre) {
  if (isInPre || el.tagName.indexOf('-') > -1) {
    baseSetAttr(el, key, value);
  } else if (isBooleanAttr(key)) {
    // set attribute for blank value
    // e.g. <option disabled>Select one</option>
    if (isFalsyAttrValue(value)) {
      el.removeAttribute(key);
    } else {
      // technically allowfullscreen is a boolean attribute for <iframe>,
      // but Flash expects a value of "true" when used on <embed> tag
      value = key === 'allowfullscreen' && el.tagName === 'EMBED'
        ? 'true'
        : key;
      el.setAttribute(key, value);
    }
  } else if (isEnumeratedAttr(key)) {
    el.setAttribute(key, convertEnumeratedValue(key, value));
  } else if (isXlink(key)) {
    if (isFalsyAttrValue(value)) {
      el.removeAttributeNS(xlinkNS, getXlinkProp(key));
    } else {
      el.setAttributeNS(xlinkNS, key, value);
    }
  } else {
    baseSetAttr(el, key, value);
  }
}

function baseSetAttr (el, key, value) {
  if (isFalsyAttrValue(value)) {
    el.removeAttribute(key);
  } else {
    // #7138: IE10 & 11 fires input event when setting placeholder on
    // <textarea>... block the first input event and remove the blocker
    // immediately.
    /* istanbul ignore if */
    if (
      isIE && !isIE9 &&
      el.tagName === 'TEXTAREA' &&
      key === 'placeholder' && value !== '' && !el.__ieph
    ) {
      var blocker = function (e) {
        e.stopImmediatePropagation();
        el.removeEventListener('input', blocker);
      };
      el.addEventListener('input', blocker);
      // $flow-disable-line
      el.__ieph = true; /* IE placeholder patched */
    }
    el.setAttribute(key, value);
  }
}

var attrs = {
  create: updateAttrs,
  update: updateAttrs
};

/*  */

function updateClass (oldVnode, vnode) {
  var el = vnode.elm;
  var data = vnode.data;
  var oldData = oldVnode.data;
  if (
    isUndef(data.staticClass) &&
    isUndef(data.class) && (
      isUndef(oldData) || (
        isUndef(oldData.staticClass) &&
        isUndef(oldData.class)
      )
    )
  ) {
    return
  }

  var cls = genClassForVnode(vnode);

  // handle transition classes
  var transitionClass = el._transitionClasses;
  if (isDef(transitionClass)) {
    cls = concat(cls, stringifyClass(transitionClass));
  }

  // set the class
  if (cls !== el._prevClass) {
    el.setAttribute('class', cls);
    el._prevClass = cls;
  }
}

var klass = {
  create: updateClass,
  update: updateClass
};

/*  */

/*  */

/*  */

/*  */

// in some cases, the event used has to be determined at runtime
// so we used some reserved tokens during compile.
var RANGE_TOKEN = '__r';
var CHECKBOX_RADIO_TOKEN = '__c';

/*  */

// normalize v-model event tokens that can only be determined at runtime.
// it's important to place the event as the first in the array because
// the whole point is ensuring the v-model callback gets called before
// user-attached handlers.
function normalizeEvents (on) {
  /* istanbul ignore if */
  if (isDef(on[RANGE_TOKEN])) {
    // IE input[type=range] only supports `change` event
    var event = isIE ? 'change' : 'input';
    on[event] = [].concat(on[RANGE_TOKEN], on[event] || []);
    delete on[RANGE_TOKEN];
  }
  // This was originally intended to fix #4521 but no longer necessary
  // after 2.5. Keeping it for backwards compat with generated code from < 2.4
  /* istanbul ignore if */
  if (isDef(on[CHECKBOX_RADIO_TOKEN])) {
    on.change = [].concat(on[CHECKBOX_RADIO_TOKEN], on.change || []);
    delete on[CHECKBOX_RADIO_TOKEN];
  }
}

var target$1;

function createOnceHandler$1 (event, handler, capture) {
  var _target = target$1; // save current target element in closure
  return function onceHandler () {
    var res = handler.apply(null, arguments);
    if (res !== null) {
      remove$2(event, onceHandler, capture, _target);
    }
  }
}

// #9446: Firefox <= 53 (in particular, ESR 52) has incorrect Event.timeStamp
// implementation and does not fire microtasks in between event propagation, so
// safe to exclude.
var useMicrotaskFix = isUsingMicroTask && !(isFF && Number(isFF[1]) <= 53);

function add$1 (
  name,
  handler,
  capture,
  passive
) {
  // async edge case #6566: inner click event triggers patch, event handler
  // attached to outer element during patch, and triggered again. This
  // happens because browsers fire microtask ticks between event propagation.
  // the solution is simple: we save the timestamp when a handler is attached,
  // and the handler would only fire if the event passed to it was fired
  // AFTER it was attached.
  if (useMicrotaskFix) {
    var attachedTimestamp = currentFlushTimestamp;
    var original = handler;
    handler = original._wrapper = function (e) {
      if (
        // no bubbling, should always fire.
        // this is just a safety net in case event.timeStamp is unreliable in
        // certain weird environments...
        e.target === e.currentTarget ||
        // event is fired after handler attachment
        e.timeStamp >= attachedTimestamp ||
        // bail for environments that have buggy event.timeStamp implementations
        // #9462 iOS 9 bug: event.timeStamp is 0 after history.pushState
        // #9681 QtWebEngine event.timeStamp is negative value
        e.timeStamp <= 0 ||
        // #9448 bail if event is fired in another document in a multi-page
        // electron/nw.js app, since event.timeStamp will be using a different
        // starting reference
        e.target.ownerDocument !== document
      ) {
        return original.apply(this, arguments)
      }
    };
  }
  target$1.addEventListener(
    name,
    handler,
    supportsPassive
      ? { capture: capture, passive: passive }
      : capture
  );
}

function remove$2 (
  name,
  handler,
  capture,
  _target
) {
  (_target || target$1).removeEventListener(
    name,
    handler._wrapper || handler,
    capture
  );
}

function updateDOMListeners (oldVnode, vnode) {
  if (isUndef(oldVnode.data.on) && isUndef(vnode.data.on)) {
    return
  }
  var on = vnode.data.on || {};
  var oldOn = oldVnode.data.on || {};
  target$1 = vnode.elm;
  normalizeEvents(on);
  updateListeners(on, oldOn, add$1, remove$2, createOnceHandler$1, vnode.context);
  target$1 = undefined;
}

var events = {
  create: updateDOMListeners,
  update: updateDOMListeners
};

/*  */

var svgContainer;

function updateDOMProps (oldVnode, vnode) {
  if (isUndef(oldVnode.data.domProps) && isUndef(vnode.data.domProps)) {
    return
  }
  var key, cur;
  var elm = vnode.elm;
  var oldProps = oldVnode.data.domProps || {};
  var props = vnode.data.domProps || {};
  // clone observed objects, as the user probably wants to mutate it
  if (isDef(props.__ob__)) {
    props = vnode.data.domProps = extend({}, props);
  }

  for (key in oldProps) {
    if (!(key in props)) {
      elm[key] = '';
    }
  }

  for (key in props) {
    cur = props[key];
    // ignore children if the node has textContent or innerHTML,
    // as these will throw away existing DOM nodes and cause removal errors
    // on subsequent patches (#3360)
    if (key === 'textContent' || key === 'innerHTML') {
      if (vnode.children) { vnode.children.length = 0; }
      if (cur === oldProps[key]) { continue }
      // #6601 work around Chrome version <= 55 bug where single textNode
      // replaced by innerHTML/textContent retains its parentNode property
      if (elm.childNodes.length === 1) {
        elm.removeChild(elm.childNodes[0]);
      }
    }

    if (key === 'value' && elm.tagName !== 'PROGRESS') {
      // store value as _value as well since
      // non-string values will be stringified
      elm._value = cur;
      // avoid resetting cursor position when value is the same
      var strCur = isUndef(cur) ? '' : String(cur);
      if (shouldUpdateValue(elm, strCur)) {
        elm.value = strCur;
      }
    } else if (key === 'innerHTML' && isSVG(elm.tagName) && isUndef(elm.innerHTML)) {
      // IE doesn't support innerHTML for SVG elements
      svgContainer = svgContainer || document.createElement('div');
      svgContainer.innerHTML = "<svg>" + cur + "</svg>";
      var svg = svgContainer.firstChild;
      while (elm.firstChild) {
        elm.removeChild(elm.firstChild);
      }
      while (svg.firstChild) {
        elm.appendChild(svg.firstChild);
      }
    } else if (
      // skip the update if old and new VDOM state is the same.
      // `value` is handled separately because the DOM value may be temporarily
      // out of sync with VDOM state due to focus, composition and modifiers.
      // This  #4521 by skipping the unnecessary `checked` update.
      cur !== oldProps[key]
    ) {
      // some property updates can throw
      // e.g. `value` on <progress> w/ non-finite value
      try {
        elm[key] = cur;
      } catch (e) {}
    }
  }
}

// check platforms/web/util/attrs.js acceptValue


function shouldUpdateValue (elm, checkVal) {
  return (!elm.composing && (
    elm.tagName === 'OPTION' ||
    isNotInFocusAndDirty(elm, checkVal) ||
    isDirtyWithModifiers(elm, checkVal)
  ))
}

function isNotInFocusAndDirty (elm, checkVal) {
  // return true when textbox (.number and .trim) loses focus and its value is
  // not equal to the updated value
  var notInFocus = true;
  // #6157
  // work around IE bug when accessing document.activeElement in an iframe
  try { notInFocus = document.activeElement !== elm; } catch (e) {}
  return notInFocus && elm.value !== checkVal
}

function isDirtyWithModifiers (elm, newVal) {
  var value = elm.value;
  var modifiers = elm._vModifiers; // injected by v-model runtime
  if (isDef(modifiers)) {
    if (modifiers.number) {
      return toNumber(value) !== toNumber(newVal)
    }
    if (modifiers.trim) {
      return value.trim() !== newVal.trim()
    }
  }
  return value !== newVal
}

var domProps = {
  create: updateDOMProps,
  update: updateDOMProps
};

/*  */

var parseStyleText = cached(function (cssText) {
  var res = {};
  var listDelimiter = /;(?![^(]*\))/g;
  var propertyDelimiter = /:(.+)/;
  cssText.split(listDelimiter).forEach(function (item) {
    if (item) {
      var tmp = item.split(propertyDelimiter);
      tmp.length > 1 && (res[tmp[0].trim()] = tmp[1].trim());
    }
  });
  return res
});

// merge static and dynamic style data on the same vnode
function normalizeStyleData (data) {
  var style = normalizeStyleBinding(data.style);
  // static style is pre-processed into an object during compilation
  // and is always a fresh object, so it's safe to merge into it
  return data.staticStyle
    ? extend(data.staticStyle, style)
    : style
}

// normalize possible array / string values into Object
function normalizeStyleBinding (bindingStyle) {
  if (Array.isArray(bindingStyle)) {
    return toObject(bindingStyle)
  }
  if (typeof bindingStyle === 'string') {
    return parseStyleText(bindingStyle)
  }
  return bindingStyle
}

/**
 * parent component style should be after child's
 * so that parent component's style could override it
 */
function getStyle (vnode, checkChild) {
  var res = {};
  var styleData;

  if (checkChild) {
    var childNode = vnode;
    while (childNode.componentInstance) {
      childNode = childNode.componentInstance._vnode;
      if (
        childNode && childNode.data &&
        (styleData = normalizeStyleData(childNode.data))
      ) {
        extend(res, styleData);
      }
    }
  }

  if ((styleData = normalizeStyleData(vnode.data))) {
    extend(res, styleData);
  }

  var parentNode = vnode;
  while ((parentNode = parentNode.parent)) {
    if (parentNode.data && (styleData = normalizeStyleData(parentNode.data))) {
      extend(res, styleData);
    }
  }
  return res
}

/*  */

var cssVarRE = /^--/;
var importantRE = /\s*!important$/;
var setProp = function (el, name, val) {
  /* istanbul ignore if */
  if (cssVarRE.test(name)) {
    el.style.setProperty(name, val);
  } else if (importantRE.test(val)) {
    el.style.setProperty(hyphenate(name), val.replace(importantRE, ''), 'important');
  } else {
    var normalizedName = normalize(name);
    if (Array.isArray(val)) {
      // Support values array created by autoprefixer, e.g.
      // {display: ["-webkit-box", "-ms-flexbox", "flex"]}
      // Set them one by one, and the browser will only set those it can recognize
      for (var i = 0, len = val.length; i < len; i++) {
        el.style[normalizedName] = val[i];
      }
    } else {
      el.style[normalizedName] = val;
    }
  }
};

var vendorNames = ['Webkit', 'Moz', 'ms'];

var emptyStyle;
var normalize = cached(function (prop) {
  emptyStyle = emptyStyle || document.createElement('div').style;
  prop = camelize(prop);
  if (prop !== 'filter' && (prop in emptyStyle)) {
    return prop
  }
  var capName = prop.charAt(0).toUpperCase() + prop.slice(1);
  for (var i = 0; i < vendorNames.length; i++) {
    var name = vendorNames[i] + capName;
    if (name in emptyStyle) {
      return name
    }
  }
});

function updateStyle (oldVnode, vnode) {
  var data = vnode.data;
  var oldData = oldVnode.data;

  if (isUndef(data.staticStyle) && isUndef(data.style) &&
    isUndef(oldData.staticStyle) && isUndef(oldData.style)
  ) {
    return
  }

  var cur, name;
  var el = vnode.elm;
  var oldStaticStyle = oldData.staticStyle;
  var oldStyleBinding = oldData.normalizedStyle || oldData.style || {};

  // if static style exists, stylebinding already merged into it when doing normalizeStyleData
  var oldStyle = oldStaticStyle || oldStyleBinding;

  var style = normalizeStyleBinding(vnode.data.style) || {};

  // store normalized style under a different key for next diff
  // make sure to clone it if it's reactive, since the user likely wants
  // to mutate it.
  vnode.data.normalizedStyle = isDef(style.__ob__)
    ? extend({}, style)
    : style;

  var newStyle = getStyle(vnode, true);

  for (name in oldStyle) {
    if (isUndef(newStyle[name])) {
      setProp(el, name, '');
    }
  }
  for (name in newStyle) {
    cur = newStyle[name];
    if (cur !== oldStyle[name]) {
      // ie9 setting to null has no effect, must use empty string
      setProp(el, name, cur == null ? '' : cur);
    }
  }
}

var style = {
  create: updateStyle,
  update: updateStyle
};

/*  */

var whitespaceRE = /\s+/;

/**
 * Add class with compatibility for SVG since classList is not supported on
 * SVG elements in IE
 */
function addClass (el, cls) {
  /* istanbul ignore if */
  if (!cls || !(cls = cls.trim())) {
    return
  }

  /* istanbul ignore else */
  if (el.classList) {
    if (cls.indexOf(' ') > -1) {
      cls.split(whitespaceRE).forEach(function (c) { return el.classList.add(c); });
    } else {
      el.classList.add(cls);
    }
  } else {
    var cur = " " + (el.getAttribute('class') || '') + " ";
    if (cur.indexOf(' ' + cls + ' ') < 0) {
      el.setAttribute('class', (cur + cls).trim());
    }
  }
}

/**
 * Remove class with compatibility for SVG since classList is not supported on
 * SVG elements in IE
 */
function removeClass (el, cls) {
  /* istanbul ignore if */
  if (!cls || !(cls = cls.trim())) {
    return
  }

  /* istanbul ignore else */
  if (el.classList) {
    if (cls.indexOf(' ') > -1) {
      cls.split(whitespaceRE).forEach(function (c) { return el.classList.remove(c); });
    } else {
      el.classList.remove(cls);
    }
    if (!el.classList.length) {
      el.removeAttribute('class');
    }
  } else {
    var cur = " " + (el.getAttribute('class') || '') + " ";
    var tar = ' ' + cls + ' ';
    while (cur.indexOf(tar) >= 0) {
      cur = cur.replace(tar, ' ');
    }
    cur = cur.trim();
    if (cur) {
      el.setAttribute('class', cur);
    } else {
      el.removeAttribute('class');
    }
  }
}

/*  */

function resolveTransition (def$$1) {
  if (!def$$1) {
    return
  }
  /* istanbul ignore else */
  if (typeof def$$1 === 'object') {
    var res = {};
    if (def$$1.css !== false) {
      extend(res, autoCssTransition(def$$1.name || 'v'));
    }
    extend(res, def$$1);
    return res
  } else if (typeof def$$1 === 'string') {
    return autoCssTransition(def$$1)
  }
}

var autoCssTransition = cached(function (name) {
  return {
    enterClass: (name + "-enter"),
    enterToClass: (name + "-enter-to"),
    enterActiveClass: (name + "-enter-active"),
    leaveClass: (name + "-leave"),
    leaveToClass: (name + "-leave-to"),
    leaveActiveClass: (name + "-leave-active")
  }
});

var hasTransition = inBrowser && !isIE9;
var TRANSITION = 'transition';
var ANIMATION = 'animation';

// Transition property/event sniffing
var transitionProp = 'transition';
var transitionEndEvent = 'transitionend';
var animationProp = 'animation';
var animationEndEvent = 'animationend';
if (hasTransition) {
  /* istanbul ignore if */
  if (window.ontransitionend === undefined &&
    window.onwebkittransitionend !== undefined
  ) {
    transitionProp = 'WebkitTransition';
    transitionEndEvent = 'webkitTransitionEnd';
  }
  if (window.onanimationend === undefined &&
    window.onwebkitanimationend !== undefined
  ) {
    animationProp = 'WebkitAnimation';
    animationEndEvent = 'webkitAnimationEnd';
  }
}

// binding to window is necessary to make hot reload work in IE in strict mode
var raf = inBrowser
  ? window.requestAnimationFrame
    ? window.requestAnimationFrame.bind(window)
    : setTimeout
  : /* istanbul ignore next */ function (fn) { return fn(); };

function nextFrame (fn) {
  raf(function () {
    raf(fn);
  });
}

function addTransitionClass (el, cls) {
  var transitionClasses = el._transitionClasses || (el._transitionClasses = []);
  if (transitionClasses.indexOf(cls) < 0) {
    transitionClasses.push(cls);
    addClass(el, cls);
  }
}

function removeTransitionClass (el, cls) {
  if (el._transitionClasses) {
    remove(el._transitionClasses, cls);
  }
  removeClass(el, cls);
}

function whenTransitionEnds (
  el,
  expectedType,
  cb
) {
  var ref = getTransitionInfo(el, expectedType);
  var type = ref.type;
  var timeout = ref.timeout;
  var propCount = ref.propCount;
  if (!type) { return cb() }
  var event = type === TRANSITION ? transitionEndEvent : animationEndEvent;
  var ended = 0;
  var end = function () {
    el.removeEventListener(event, onEnd);
    cb();
  };
  var onEnd = function (e) {
    if (e.target === el) {
      if (++ended >= propCount) {
        end();
      }
    }
  };
  setTimeout(function () {
    if (ended < propCount) {
      end();
    }
  }, timeout + 1);
  el.addEventListener(event, onEnd);
}

var transformRE = /\b(transform|all)(,|$)/;

function getTransitionInfo (el, expectedType) {
  var styles = window.getComputedStyle(el);
  // JSDOM may return undefined for transition properties
  var transitionDelays = (styles[transitionProp + 'Delay'] || '').split(', ');
  var transitionDurations = (styles[transitionProp + 'Duration'] || '').split(', ');
  var transitionTimeout = getTimeout(transitionDelays, transitionDurations);
  var animationDelays = (styles[animationProp + 'Delay'] || '').split(', ');
  var animationDurations = (styles[animationProp + 'Duration'] || '').split(', ');
  var animationTimeout = getTimeout(animationDelays, animationDurations);

  var type;
  var timeout = 0;
  var propCount = 0;
  /* istanbul ignore if */
  if (expectedType === TRANSITION) {
    if (transitionTimeout > 0) {
      type = TRANSITION;
      timeout = transitionTimeout;
      propCount = transitionDurations.length;
    }
  } else if (expectedType === ANIMATION) {
    if (animationTimeout > 0) {
      type = ANIMATION;
      timeout = animationTimeout;
      propCount = animationDurations.length;
    }
  } else {
    timeout = Math.max(transitionTimeout, animationTimeout);
    type = timeout > 0
      ? transitionTimeout > animationTimeout
        ? TRANSITION
        : ANIMATION
      : null;
    propCount = type
      ? type === TRANSITION
        ? transitionDurations.length
        : animationDurations.length
      : 0;
  }
  var hasTransform =
    type === TRANSITION &&
    transformRE.test(styles[transitionProp + 'Property']);
  return {
    type: type,
    timeout: timeout,
    propCount: propCount,
    hasTransform: hasTransform
  }
}

function getTimeout (delays, durations) {
  /* istanbul ignore next */
  while (delays.length < durations.length) {
    delays = delays.concat(delays);
  }

  return Math.max.apply(null, durations.map(function (d, i) {
    return toMs(d) + toMs(delays[i])
  }))
}

// Old versions of Chromium (below 61.0.3163.100) formats floating pointer numbers
// in a locale-dependent way, using a comma instead of a dot.
// If comma is not replaced with a dot, the input will be rounded down (i.e. acting
// as a floor function) causing unexpected behaviors
function toMs (s) {
  return Number(s.slice(0, -1).replace(',', '.')) * 1000
}

/*  */

function enter (vnode, toggleDisplay) {
  var el = vnode.elm;

  // call leave callback now
  if (isDef(el._leaveCb)) {
    el._leaveCb.cancelled = true;
    el._leaveCb();
  }

  var data = resolveTransition(vnode.data.transition);
  if (isUndef(data)) {
    return
  }

  /* istanbul ignore if */
  if (isDef(el._enterCb) || el.nodeType !== 1) {
    return
  }

  var css = data.css;
  var type = data.type;
  var enterClass = data.enterClass;
  var enterToClass = data.enterToClass;
  var enterActiveClass = data.enterActiveClass;
  var appearClass = data.appearClass;
  var appearToClass = data.appearToClass;
  var appearActiveClass = data.appearActiveClass;
  var beforeEnter = data.beforeEnter;
  var enter = data.enter;
  var afterEnter = data.afterEnter;
  var enterCancelled = data.enterCancelled;
  var beforeAppear = data.beforeAppear;
  var appear = data.appear;
  var afterAppear = data.afterAppear;
  var appearCancelled = data.appearCancelled;
  var duration = data.duration;

  // activeInstance will always be the <transition> component managing this
  // transition. One edge case to check is when the <transition> is placed
  // as the root node of a child component. In that case we need to check
  // <transition>'s parent for appear check.
  var context = activeInstance;
  var transitionNode = activeInstance.$vnode;
  while (transitionNode && transitionNode.parent) {
    context = transitionNode.context;
    transitionNode = transitionNode.parent;
  }

  var isAppear = !context._isMounted || !vnode.isRootInsert;

  if (isAppear && !appear && appear !== '') {
    return
  }

  var startClass = isAppear && appearClass
    ? appearClass
    : enterClass;
  var activeClass = isAppear && appearActiveClass
    ? appearActiveClass
    : enterActiveClass;
  var toClass = isAppear && appearToClass
    ? appearToClass
    : enterToClass;

  var beforeEnterHook = isAppear
    ? (beforeAppear || beforeEnter)
    : beforeEnter;
  var enterHook = isAppear
    ? (typeof appear === 'function' ? appear : enter)
    : enter;
  var afterEnterHook = isAppear
    ? (afterAppear || afterEnter)
    : afterEnter;
  var enterCancelledHook = isAppear
    ? (appearCancelled || enterCancelled)
    : enterCancelled;

  var explicitEnterDuration = toNumber(
    isObject(duration)
      ? duration.enter
      : duration
  );

  if ( true && explicitEnterDuration != null) {
    checkDuration(explicitEnterDuration, 'enter', vnode);
  }

  var expectsCSS = css !== false && !isIE9;
  var userWantsControl = getHookArgumentsLength(enterHook);

  var cb = el._enterCb = once(function () {
    if (expectsCSS) {
      removeTransitionClass(el, toClass);
      removeTransitionClass(el, activeClass);
    }
    if (cb.cancelled) {
      if (expectsCSS) {
        removeTransitionClass(el, startClass);
      }
      enterCancelledHook && enterCancelledHook(el);
    } else {
      afterEnterHook && afterEnterHook(el);
    }
    el._enterCb = null;
  });

  if (!vnode.data.show) {
    // remove pending leave element on enter by injecting an insert hook
    mergeVNodeHook(vnode, 'insert', function () {
      var parent = el.parentNode;
      var pendingNode = parent && parent._pending && parent._pending[vnode.key];
      if (pendingNode &&
        pendingNode.tag === vnode.tag &&
        pendingNode.elm._leaveCb
      ) {
        pendingNode.elm._leaveCb();
      }
      enterHook && enterHook(el, cb);
    });
  }

  // start enter transition
  beforeEnterHook && beforeEnterHook(el);
  if (expectsCSS) {
    addTransitionClass(el, startClass);
    addTransitionClass(el, activeClass);
    nextFrame(function () {
      removeTransitionClass(el, startClass);
      if (!cb.cancelled) {
        addTransitionClass(el, toClass);
        if (!userWantsControl) {
          if (isValidDuration(explicitEnterDuration)) {
            setTimeout(cb, explicitEnterDuration);
          } else {
            whenTransitionEnds(el, type, cb);
          }
        }
      }
    });
  }

  if (vnode.data.show) {
    toggleDisplay && toggleDisplay();
    enterHook && enterHook(el, cb);
  }

  if (!expectsCSS && !userWantsControl) {
    cb();
  }
}

function leave (vnode, rm) {
  var el = vnode.elm;

  // call enter callback now
  if (isDef(el._enterCb)) {
    el._enterCb.cancelled = true;
    el._enterCb();
  }

  var data = resolveTransition(vnode.data.transition);
  if (isUndef(data) || el.nodeType !== 1) {
    return rm()
  }

  /* istanbul ignore if */
  if (isDef(el._leaveCb)) {
    return
  }

  var css = data.css;
  var type = data.type;
  var leaveClass = data.leaveClass;
  var leaveToClass = data.leaveToClass;
  var leaveActiveClass = data.leaveActiveClass;
  var beforeLeave = data.beforeLeave;
  var leave = data.leave;
  var afterLeave = data.afterLeave;
  var leaveCancelled = data.leaveCancelled;
  var delayLeave = data.delayLeave;
  var duration = data.duration;

  var expectsCSS = css !== false && !isIE9;
  var userWantsControl = getHookArgumentsLength(leave);

  var explicitLeaveDuration = toNumber(
    isObject(duration)
      ? duration.leave
      : duration
  );

  if ( true && isDef(explicitLeaveDuration)) {
    checkDuration(explicitLeaveDuration, 'leave', vnode);
  }

  var cb = el._leaveCb = once(function () {
    if (el.parentNode && el.parentNode._pending) {
      el.parentNode._pending[vnode.key] = null;
    }
    if (expectsCSS) {
      removeTransitionClass(el, leaveToClass);
      removeTransitionClass(el, leaveActiveClass);
    }
    if (cb.cancelled) {
      if (expectsCSS) {
        removeTransitionClass(el, leaveClass);
      }
      leaveCancelled && leaveCancelled(el);
    } else {
      rm();
      afterLeave && afterLeave(el);
    }
    el._leaveCb = null;
  });

  if (delayLeave) {
    delayLeave(performLeave);
  } else {
    performLeave();
  }

  function performLeave () {
    // the delayed leave may have already been cancelled
    if (cb.cancelled) {
      return
    }
    // record leaving element
    if (!vnode.data.show && el.parentNode) {
      (el.parentNode._pending || (el.parentNode._pending = {}))[(vnode.key)] = vnode;
    }
    beforeLeave && beforeLeave(el);
    if (expectsCSS) {
      addTransitionClass(el, leaveClass);
      addTransitionClass(el, leaveActiveClass);
      nextFrame(function () {
        removeTransitionClass(el, leaveClass);
        if (!cb.cancelled) {
          addTransitionClass(el, leaveToClass);
          if (!userWantsControl) {
            if (isValidDuration(explicitLeaveDuration)) {
              setTimeout(cb, explicitLeaveDuration);
            } else {
              whenTransitionEnds(el, type, cb);
            }
          }
        }
      });
    }
    leave && leave(el, cb);
    if (!expectsCSS && !userWantsControl) {
      cb();
    }
  }
}

// only used in dev mode
function checkDuration (val, name, vnode) {
  if (typeof val !== 'number') {
    warn(
      "<transition> explicit " + name + " duration is not a valid number - " +
      "got " + (JSON.stringify(val)) + ".",
      vnode.context
    );
  } else if (isNaN(val)) {
    warn(
      "<transition> explicit " + name + " duration is NaN - " +
      'the duration expression might be incorrect.',
      vnode.context
    );
  }
}

function isValidDuration (val) {
  return typeof val === 'number' && !isNaN(val)
}

/**
 * Normalize a transition hook's argument length. The hook may be:
 * - a merged hook (invoker) with the original in .fns
 * - a wrapped component method (check ._length)
 * - a plain function (.length)
 */
function getHookArgumentsLength (fn) {
  if (isUndef(fn)) {
    return false
  }
  var invokerFns = fn.fns;
  if (isDef(invokerFns)) {
    // invoker
    return getHookArgumentsLength(
      Array.isArray(invokerFns)
        ? invokerFns[0]
        : invokerFns
    )
  } else {
    return (fn._length || fn.length) > 1
  }
}

function _enter (_, vnode) {
  if (vnode.data.show !== true) {
    enter(vnode);
  }
}

var transition = inBrowser ? {
  create: _enter,
  activate: _enter,
  remove: function remove$$1 (vnode, rm) {
    /* istanbul ignore else */
    if (vnode.data.show !== true) {
      leave(vnode, rm);
    } else {
      rm();
    }
  }
} : {};

var platformModules = [
  attrs,
  klass,
  events,
  domProps,
  style,
  transition
];

/*  */

// the directive module should be applied last, after all
// built-in modules have been applied.
var modules = platformModules.concat(baseModules);

var patch = createPatchFunction({ nodeOps: nodeOps, modules: modules });

/**
 * Not type checking this file because flow doesn't like attaching
 * properties to Elements.
 */

/* istanbul ignore if */
if (isIE9) {
  // http://www.matts411.com/post/internet-explorer-9-oninput/
  document.addEventListener('selectionchange', function () {
    var el = document.activeElement;
    if (el && el.vmodel) {
      trigger(el, 'input');
    }
  });
}

var directive = {
  inserted: function inserted (el, binding, vnode, oldVnode) {
    if (vnode.tag === 'select') {
      // #6903
      if (oldVnode.elm && !oldVnode.elm._vOptions) {
        mergeVNodeHook(vnode, 'postpatch', function () {
          directive.componentUpdated(el, binding, vnode);
        });
      } else {
        setSelected(el, binding, vnode.context);
      }
      el._vOptions = [].map.call(el.options, getValue);
    } else if (vnode.tag === 'textarea' || isTextInputType(el.type)) {
      el._vModifiers = binding.modifiers;
      if (!binding.modifiers.lazy) {
        el.addEventListener('compositionstart', onCompositionStart);
        el.addEventListener('compositionend', onCompositionEnd);
        // Safari < 10.2 & UIWebView doesn't fire compositionend when
        // switching focus before confirming composition choice
        // this also fixes the issue where some browsers e.g. iOS Chrome
        // fires "change" instead of "input" on autocomplete.
        el.addEventListener('change', onCompositionEnd);
        /* istanbul ignore if */
        if (isIE9) {
          el.vmodel = true;
        }
      }
    }
  },

  componentUpdated: function componentUpdated (el, binding, vnode) {
    if (vnode.tag === 'select') {
      setSelected(el, binding, vnode.context);
      // in case the options rendered by v-for have changed,
      // it's possible that the value is out-of-sync with the rendered options.
      // detect such cases and filter out values that no longer has a matching
      // option in the DOM.
      var prevOptions = el._vOptions;
      var curOptions = el._vOptions = [].map.call(el.options, getValue);
      if (curOptions.some(function (o, i) { return !looseEqual(o, prevOptions[i]); })) {
        // trigger change event if
        // no matching option found for at least one value
        var needReset = el.multiple
          ? binding.value.some(function (v) { return hasNoMatchingOption(v, curOptions); })
          : binding.value !== binding.oldValue && hasNoMatchingOption(binding.value, curOptions);
        if (needReset) {
          trigger(el, 'change');
        }
      }
    }
  }
};

function setSelected (el, binding, vm) {
  actuallySetSelected(el, binding, vm);
  /* istanbul ignore if */
  if (isIE || isEdge) {
    setTimeout(function () {
      actuallySetSelected(el, binding, vm);
    }, 0);
  }
}

function actuallySetSelected (el, binding, vm) {
  var value = binding.value;
  var isMultiple = el.multiple;
  if (isMultiple && !Array.isArray(value)) {
     true && warn(
      "<select multiple v-model=\"" + (binding.expression) + "\"> " +
      "expects an Array value for its binding, but got " + (Object.prototype.toString.call(value).slice(8, -1)),
      vm
    );
    return
  }
  var selected, option;
  for (var i = 0, l = el.options.length; i < l; i++) {
    option = el.options[i];
    if (isMultiple) {
      selected = looseIndexOf(value, getValue(option)) > -1;
      if (option.selected !== selected) {
        option.selected = selected;
      }
    } else {
      if (looseEqual(getValue(option), value)) {
        if (el.selectedIndex !== i) {
          el.selectedIndex = i;
        }
        return
      }
    }
  }
  if (!isMultiple) {
    el.selectedIndex = -1;
  }
}

function hasNoMatchingOption (value, options) {
  return options.every(function (o) { return !looseEqual(o, value); })
}

function getValue (option) {
  return '_value' in option
    ? option._value
    : option.value
}

function onCompositionStart (e) {
  e.target.composing = true;
}

function onCompositionEnd (e) {
  // prevent triggering an input event for no reason
  if (!e.target.composing) { return }
  e.target.composing = false;
  trigger(e.target, 'input');
}

function trigger (el, type) {
  var e = document.createEvent('HTMLEvents');
  e.initEvent(type, true, true);
  el.dispatchEvent(e);
}

/*  */

// recursively search for possible transition defined inside the component root
function locateNode (vnode) {
  return vnode.componentInstance && (!vnode.data || !vnode.data.transition)
    ? locateNode(vnode.componentInstance._vnode)
    : vnode
}

var show = {
  bind: function bind (el, ref, vnode) {
    var value = ref.value;

    vnode = locateNode(vnode);
    var transition$$1 = vnode.data && vnode.data.transition;
    var originalDisplay = el.__vOriginalDisplay =
      el.style.display === 'none' ? '' : el.style.display;
    if (value && transition$$1) {
      vnode.data.show = true;
      enter(vnode, function () {
        el.style.display = originalDisplay;
      });
    } else {
      el.style.display = value ? originalDisplay : 'none';
    }
  },

  update: function update (el, ref, vnode) {
    var value = ref.value;
    var oldValue = ref.oldValue;

    /* istanbul ignore if */
    if (!value === !oldValue) { return }
    vnode = locateNode(vnode);
    var transition$$1 = vnode.data && vnode.data.transition;
    if (transition$$1) {
      vnode.data.show = true;
      if (value) {
        enter(vnode, function () {
          el.style.display = el.__vOriginalDisplay;
        });
      } else {
        leave(vnode, function () {
          el.style.display = 'none';
        });
      }
    } else {
      el.style.display = value ? el.__vOriginalDisplay : 'none';
    }
  },

  unbind: function unbind (
    el,
    binding,
    vnode,
    oldVnode,
    isDestroy
  ) {
    if (!isDestroy) {
      el.style.display = el.__vOriginalDisplay;
    }
  }
};

var platformDirectives = {
  model: directive,
  show: show
};

/*  */

var transitionProps = {
  name: String,
  appear: Boolean,
  css: Boolean,
  mode: String,
  type: String,
  enterClass: String,
  leaveClass: String,
  enterToClass: String,
  leaveToClass: String,
  enterActiveClass: String,
  leaveActiveClass: String,
  appearClass: String,
  appearActiveClass: String,
  appearToClass: String,
  duration: [Number, String, Object]
};

// in case the child is also an abstract component, e.g. <keep-alive>
// we want to recursively retrieve the real component to be rendered
function getRealChild (vnode) {
  var compOptions = vnode && vnode.componentOptions;
  if (compOptions && compOptions.Ctor.options.abstract) {
    return getRealChild(getFirstComponentChild(compOptions.children))
  } else {
    return vnode
  }
}

function extractTransitionData (comp) {
  var data = {};
  var options = comp.$options;
  // props
  for (var key in options.propsData) {
    data[key] = comp[key];
  }
  // events.
  // extract listeners and pass them directly to the transition methods
  var listeners = options._parentListeners;
  for (var key$1 in listeners) {
    data[camelize(key$1)] = listeners[key$1];
  }
  return data
}

function placeholder (h, rawChild) {
  if (/\d-keep-alive$/.test(rawChild.tag)) {
    return h('keep-alive', {
      props: rawChild.componentOptions.propsData
    })
  }
}

function hasParentTransition (vnode) {
  while ((vnode = vnode.parent)) {
    if (vnode.data.transition) {
      return true
    }
  }
}

function isSameChild (child, oldChild) {
  return oldChild.key === child.key && oldChild.tag === child.tag
}

var isNotTextNode = function (c) { return c.tag || isAsyncPlaceholder(c); };

var isVShowDirective = function (d) { return d.name === 'show'; };

var Transition = {
  name: 'transition',
  props: transitionProps,
  abstract: true,

  render: function render (h) {
    var this$1 = this;

    var children = this.$slots.default;
    if (!children) {
      return
    }

    // filter out text nodes (possible whitespaces)
    children = children.filter(isNotTextNode);
    /* istanbul ignore if */
    if (!children.length) {
      return
    }

    // warn multiple elements
    if ( true && children.length > 1) {
      warn(
        '<transition> can only be used on a single element. Use ' +
        '<transition-group> for lists.',
        this.$parent
      );
    }

    var mode = this.mode;

    // warn invalid mode
    if ( true &&
      mode && mode !== 'in-out' && mode !== 'out-in'
    ) {
      warn(
        'invalid <transition> mode: ' + mode,
        this.$parent
      );
    }

    var rawChild = children[0];

    // if this is a component root node and the component's
    // parent container node also has transition, skip.
    if (hasParentTransition(this.$vnode)) {
      return rawChild
    }

    // apply transition data to child
    // use getRealChild() to ignore abstract components e.g. keep-alive
    var child = getRealChild(rawChild);
    /* istanbul ignore if */
    if (!child) {
      return rawChild
    }

    if (this._leaving) {
      return placeholder(h, rawChild)
    }

    // ensure a key that is unique to the vnode type and to this transition
    // component instance. This key will be used to remove pending leaving nodes
    // during entering.
    var id = "__transition-" + (this._uid) + "-";
    child.key = child.key == null
      ? child.isComment
        ? id + 'comment'
        : id + child.tag
      : isPrimitive(child.key)
        ? (String(child.key).indexOf(id) === 0 ? child.key : id + child.key)
        : child.key;

    var data = (child.data || (child.data = {})).transition = extractTransitionData(this);
    var oldRawChild = this._vnode;
    var oldChild = getRealChild(oldRawChild);

    // mark v-show
    // so that the transition module can hand over the control to the directive
    if (child.data.directives && child.data.directives.some(isVShowDirective)) {
      child.data.show = true;
    }

    if (
      oldChild &&
      oldChild.data &&
      !isSameChild(child, oldChild) &&
      !isAsyncPlaceholder(oldChild) &&
      // #6687 component root is a comment node
      !(oldChild.componentInstance && oldChild.componentInstance._vnode.isComment)
    ) {
      // replace old child transition data with fresh one
      // important for dynamic transitions!
      var oldData = oldChild.data.transition = extend({}, data);
      // handle transition mode
      if (mode === 'out-in') {
        // return placeholder node and queue update when leave finishes
        this._leaving = true;
        mergeVNodeHook(oldData, 'afterLeave', function () {
          this$1._leaving = false;
          this$1.$forceUpdate();
        });
        return placeholder(h, rawChild)
      } else if (mode === 'in-out') {
        if (isAsyncPlaceholder(child)) {
          return oldRawChild
        }
        var delayedLeave;
        var performLeave = function () { delayedLeave(); };
        mergeVNodeHook(data, 'afterEnter', performLeave);
        mergeVNodeHook(data, 'enterCancelled', performLeave);
        mergeVNodeHook(oldData, 'delayLeave', function (leave) { delayedLeave = leave; });
      }
    }

    return rawChild
  }
};

/*  */

var props = extend({
  tag: String,
  moveClass: String
}, transitionProps);

delete props.mode;

var TransitionGroup = {
  props: props,

  beforeMount: function beforeMount () {
    var this$1 = this;

    var update = this._update;
    this._update = function (vnode, hydrating) {
      var restoreActiveInstance = setActiveInstance(this$1);
      // force removing pass
      this$1.__patch__(
        this$1._vnode,
        this$1.kept,
        false, // hydrating
        true // removeOnly (!important, avoids unnecessary moves)
      );
      this$1._vnode = this$1.kept;
      restoreActiveInstance();
      update.call(this$1, vnode, hydrating);
    };
  },

  render: function render (h) {
    var tag = this.tag || this.$vnode.data.tag || 'span';
    var map = Object.create(null);
    var prevChildren = this.prevChildren = this.children;
    var rawChildren = this.$slots.default || [];
    var children = this.children = [];
    var transitionData = extractTransitionData(this);

    for (var i = 0; i < rawChildren.length; i++) {
      var c = rawChildren[i];
      if (c.tag) {
        if (c.key != null && String(c.key).indexOf('__vlist') !== 0) {
          children.push(c);
          map[c.key] = c
          ;(c.data || (c.data = {})).transition = transitionData;
        } else if (true) {
          var opts = c.componentOptions;
          var name = opts ? (opts.Ctor.options.name || opts.tag || '') : c.tag;
          warn(("<transition-group> children must be keyed: <" + name + ">"));
        }
      }
    }

    if (prevChildren) {
      var kept = [];
      var removed = [];
      for (var i$1 = 0; i$1 < prevChildren.length; i$1++) {
        var c$1 = prevChildren[i$1];
        c$1.data.transition = transitionData;
        c$1.data.pos = c$1.elm.getBoundingClientRect();
        if (map[c$1.key]) {
          kept.push(c$1);
        } else {
          removed.push(c$1);
        }
      }
      this.kept = h(tag, null, kept);
      this.removed = removed;
    }

    return h(tag, null, children)
  },

  updated: function updated () {
    var children = this.prevChildren;
    var moveClass = this.moveClass || ((this.name || 'v') + '-move');
    if (!children.length || !this.hasMove(children[0].elm, moveClass)) {
      return
    }

    // we divide the work into three loops to avoid mixing DOM reads and writes
    // in each iteration - which helps prevent layout thrashing.
    children.forEach(callPendingCbs);
    children.forEach(recordPosition);
    children.forEach(applyTranslation);

    // force reflow to put everything in position
    // assign to this to avoid being removed in tree-shaking
    // $flow-disable-line
    this._reflow = document.body.offsetHeight;

    children.forEach(function (c) {
      if (c.data.moved) {
        var el = c.elm;
        var s = el.style;
        addTransitionClass(el, moveClass);
        s.transform = s.WebkitTransform = s.transitionDuration = '';
        el.addEventListener(transitionEndEvent, el._moveCb = function cb (e) {
          if (e && e.target !== el) {
            return
          }
          if (!e || /transform$/.test(e.propertyName)) {
            el.removeEventListener(transitionEndEvent, cb);
            el._moveCb = null;
            removeTransitionClass(el, moveClass);
          }
        });
      }
    });
  },

  methods: {
    hasMove: function hasMove (el, moveClass) {
      /* istanbul ignore if */
      if (!hasTransition) {
        return false
      }
      /* istanbul ignore if */
      if (this._hasMove) {
        return this._hasMove
      }
      // Detect whether an element with the move class applied has
      // CSS transitions. Since the element may be inside an entering
      // transition at this very moment, we make a clone of it and remove
      // all other transition classes applied to ensure only the move class
      // is applied.
      var clone = el.cloneNode();
      if (el._transitionClasses) {
        el._transitionClasses.forEach(function (cls) { removeClass(clone, cls); });
      }
      addClass(clone, moveClass);
      clone.style.display = 'none';
      this.$el.appendChild(clone);
      var info = getTransitionInfo(clone);
      this.$el.removeChild(clone);
      return (this._hasMove = info.hasTransform)
    }
  }
};

function callPendingCbs (c) {
  /* istanbul ignore if */
  if (c.elm._moveCb) {
    c.elm._moveCb();
  }
  /* istanbul ignore if */
  if (c.elm._enterCb) {
    c.elm._enterCb();
  }
}

function recordPosition (c) {
  c.data.newPos = c.elm.getBoundingClientRect();
}

function applyTranslation (c) {
  var oldPos = c.data.pos;
  var newPos = c.data.newPos;
  var dx = oldPos.left - newPos.left;
  var dy = oldPos.top - newPos.top;
  if (dx || dy) {
    c.data.moved = true;
    var s = c.elm.style;
    s.transform = s.WebkitTransform = "translate(" + dx + "px," + dy + "px)";
    s.transitionDuration = '0s';
  }
}

var platformComponents = {
  Transition: Transition,
  TransitionGroup: TransitionGroup
};

/*  */

// install platform specific utils
Vue.config.mustUseProp = mustUseProp;
Vue.config.isReservedTag = isReservedTag;
Vue.config.isReservedAttr = isReservedAttr;
Vue.config.getTagNamespace = getTagNamespace;
Vue.config.isUnknownElement = isUnknownElement;

// install platform runtime directives & components
extend(Vue.options.directives, platformDirectives);
extend(Vue.options.components, platformComponents);

// install platform patch function
Vue.prototype.__patch__ = inBrowser ? patch : noop;

// public mount method
Vue.prototype.$mount = function (
  el,
  hydrating
) {
  el = el && inBrowser ? query(el) : undefined;
  return mountComponent(this, el, hydrating)
};

// devtools global hook
/* istanbul ignore next */
if (inBrowser) {
  setTimeout(function () {
    if (config.devtools) {
      if (devtools) {
        devtools.emit('init', Vue);
      } else if (
        true
      ) {
        console[console.info ? 'info' : 'log'](
          'Download the Vue Devtools extension for a better development experience:\n' +
          'https://github.com/vuejs/vue-devtools'
        );
      }
    }
    if ( true &&
      config.productionTip !== false &&
      typeof console !== 'undefined'
    ) {
      console[console.info ? 'info' : 'log'](
        "You are running Vue in development mode.\n" +
        "Make sure to turn on production mode when deploying for production.\n" +
        "See more tips at https://vuejs.org/guide/deployment.html"
      );
    }
  }, 0);
}

/*  */

/* harmony default export */ const __WEBPACK_DEFAULT_EXPORT__ = (Vue);


/***/ })

/******/ 	});
/************************************************************************/
/******/ 	// The module cache
/******/ 	var __webpack_module_cache__ = {};
/******/ 	
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/ 		// Check if module is in cache
/******/ 		var cachedModule = __webpack_module_cache__[moduleId];
/******/ 		if (cachedModule !== undefined) {
/******/ 			return cachedModule.exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = __webpack_module_cache__[moduleId] = {
/******/ 			// no module.id needed
/******/ 			// no module.loaded needed
/******/ 			exports: {}
/******/ 		};
/******/ 	
/******/ 		// Execute the module function
/******/ 		__webpack_modules__[moduleId](module, module.exports, __webpack_require__);
/******/ 	
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/ 	
/************************************************************************/
/******/ 	/* webpack/runtime/define property getters */
/******/ 	(() => {
/******/ 		// define getter functions for harmony exports
/******/ 		__webpack_require__.d = (exports, definition) => {
/******/ 			for(var key in definition) {
/******/ 				if(__webpack_require__.o(definition, key) && !__webpack_require__.o(exports, key)) {
/******/ 					Object.defineProperty(exports, key, { enumerable: true, get: definition[key] });
/******/ 				}
/******/ 			}
/******/ 		};
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/global */
/******/ 	(() => {
/******/ 		__webpack_require__.g = (function() {
/******/ 			if (typeof globalThis === 'object') return globalThis;
/******/ 			try {
/******/ 				return this || new Function('return this')();
/******/ 			} catch (e) {
/******/ 				if (typeof window === 'object') return window;
/******/ 			}
/******/ 		})();
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/hasOwnProperty shorthand */
/******/ 	(() => {
/******/ 		__webpack_require__.o = (obj, prop) => (Object.prototype.hasOwnProperty.call(obj, prop))
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/make namespace object */
/******/ 	(() => {
/******/ 		// define __esModule on exports
/******/ 		__webpack_require__.r = (exports) => {
/******/ 			if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 				Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 			}
/******/ 			Object.defineProperty(exports, '__esModule', { value: true });
/******/ 		};
/******/ 	})();
/******/ 	
/************************************************************************/
var __webpack_exports__ = {};
// This entry need to be wrapped in an IIFE because it need to be in strict mode.
(() => {
"use strict";
/*!****************************************!*\
  !*** ./src/components/copybox/main.ts ***!
  \****************************************/
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var vue__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! vue */ "./node_modules/vue/dist/vue.runtime.esm.js");
/* harmony import */ var _rundeck_ui_trellis_lib_components_containers_copybox_CopyBox_vue__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @rundeck/ui-trellis/lib/components/containers/copybox/CopyBox.vue */ "../ui-trellis/lib/components/containers/copybox/CopyBox.vue.js");

 // The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.

window.addEventListener('DOMContentLoaded', init);

function init() {
  var els = document.body.getElementsByClassName('vue-copybox');
  if (!els) return;

  for (var i = 0; i < els.length; i++) {
    var e = els[i];
    /* eslint-disable no-new */

    new vue__WEBPACK_IMPORTED_MODULE_1__["default"]({
      el: e,
      components: {
        copyBox: _rundeck_ui_trellis_lib_components_containers_copybox_CopyBox_vue__WEBPACK_IMPORTED_MODULE_0__
      }
    });
  }
}
})();

/******/ })()
;