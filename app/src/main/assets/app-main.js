(function () {
  'use strict';

  /**
   * @license
   * Copyright 2019 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   */
  const t$3=globalThis,e$4=t$3.ShadowRoot&&(void 0===t$3.ShadyCSS||t$3.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,s$3=Symbol(),o$3=new WeakMap;let n$2 = class n{constructor(t,e,o){if(this._$cssResult$=!0,o!==s$3)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t,this.t=e;}get styleSheet(){let t=this.o;const s=this.t;if(e$4&&void 0===t){const e=void 0!==s&&1===s.length;e&&(t=o$3.get(s)),void 0===t&&((this.o=t=new CSSStyleSheet).replaceSync(this.cssText),e&&o$3.set(s,t));}return t}toString(){return this.cssText}};const r$4=t=>new n$2("string"==typeof t?t:t+"",void 0,s$3),i$3=(t,...e)=>{const o=1===t.length?t[0]:e.reduce(((e,s,o)=>e+(t=>{if(!0===t._$cssResult$)return t.cssText;if("number"==typeof t)return t;throw Error("Value passed to 'css' function must be a 'css' function result: "+t+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(s)+t[o+1]),t[0]);return new n$2(o,t,s$3)},S$1=(s,o)=>{if(e$4)s.adoptedStyleSheets=o.map((t=>t instanceof CSSStyleSheet?t:t.styleSheet));else for(const e of o){const o=document.createElement("style"),n=t$3.litNonce;void 0!==n&&o.setAttribute("nonce",n),o.textContent=e.cssText,s.appendChild(o);}},c$3=e$4?t=>t:t=>t instanceof CSSStyleSheet?(t=>{let e="";for(const s of t.cssRules)e+=s.cssText;return r$4(e)})(t):t;

  /**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   */const{is:i$2,defineProperty:e$3,getOwnPropertyDescriptor:r$3,getOwnPropertyNames:h$2,getOwnPropertySymbols:o$2,getPrototypeOf:n$1}=Object,a$1=globalThis,c$2=a$1.trustedTypes,l$1=c$2?c$2.emptyScript:"",p$2=a$1.reactiveElementPolyfillSupport,d$1=(t,s)=>t,u$3={toAttribute(t,s){switch(s){case Boolean:t=t?l$1:null;break;case Object:case Array:t=null==t?t:JSON.stringify(t);}return t},fromAttribute(t,s){let i=t;switch(s){case Boolean:i=null!==t;break;case Number:i=null===t?null:Number(t);break;case Object:case Array:try{i=JSON.parse(t);}catch(t){i=null;}}return i}},f$1=(t,s)=>!i$2(t,s),y$1={attribute:!0,type:String,converter:u$3,reflect:!1,hasChanged:f$1};Symbol.metadata??=Symbol("metadata"),a$1.litPropertyMetadata??=new WeakMap;class b extends HTMLElement{static addInitializer(t){this._$Ei(),(this.l??=[]).push(t);}static get observedAttributes(){return this.finalize(),this._$Eh&&[...this._$Eh.keys()]}static createProperty(t,s=y$1){if(s.state&&(s.attribute=!1),this._$Ei(),this.elementProperties.set(t,s),!s.noAccessor){const i=Symbol(),r=this.getPropertyDescriptor(t,i,s);void 0!==r&&e$3(this.prototype,t,r);}}static getPropertyDescriptor(t,s,i){const{get:e,set:h}=r$3(this.prototype,t)??{get(){return this[s]},set(t){this[s]=t;}};return {get(){return e?.call(this)},set(s){const r=e?.call(this);h.call(this,s),this.requestUpdate(t,r,i);},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)??y$1}static _$Ei(){if(this.hasOwnProperty(d$1("elementProperties")))return;const t=n$1(this);t.finalize(),void 0!==t.l&&(this.l=[...t.l]),this.elementProperties=new Map(t.elementProperties);}static finalize(){if(this.hasOwnProperty(d$1("finalized")))return;if(this.finalized=!0,this._$Ei(),this.hasOwnProperty(d$1("properties"))){const t=this.properties,s=[...h$2(t),...o$2(t)];for(const i of s)this.createProperty(i,t[i]);}const t=this[Symbol.metadata];if(null!==t){const s=litPropertyMetadata.get(t);if(void 0!==s)for(const[t,i]of s)this.elementProperties.set(t,i);}this._$Eh=new Map;for(const[t,s]of this.elementProperties){const i=this._$Eu(t,s);void 0!==i&&this._$Eh.set(i,t);}this.elementStyles=this.finalizeStyles(this.styles);}static finalizeStyles(s){const i=[];if(Array.isArray(s)){const e=new Set(s.flat(1/0).reverse());for(const s of e)i.unshift(c$3(s));}else void 0!==s&&i.push(c$3(s));return i}static _$Eu(t,s){const i=s.attribute;return !1===i?void 0:"string"==typeof i?i:"string"==typeof t?t.toLowerCase():void 0}constructor(){super(),this._$Ep=void 0,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Em=null,this._$Ev();}_$Ev(){this._$Eg=new Promise((t=>this.enableUpdating=t)),this._$AL=new Map,this._$ES(),this.requestUpdate(),this.constructor.l?.forEach((t=>t(this)));}addController(t){(this._$E_??=new Set).add(t),void 0!==this.renderRoot&&this.isConnected&&t.hostConnected?.();}removeController(t){this._$E_?.delete(t);}_$ES(){const t=new Map,s=this.constructor.elementProperties;for(const i of s.keys())this.hasOwnProperty(i)&&(t.set(i,this[i]),delete this[i]);t.size>0&&(this._$Ep=t);}createRenderRoot(){const t=this.shadowRoot??this.attachShadow(this.constructor.shadowRootOptions);return S$1(t,this.constructor.elementStyles),t}connectedCallback(){this.renderRoot??=this.createRenderRoot(),this.enableUpdating(!0),this._$E_?.forEach((t=>t.hostConnected?.()));}enableUpdating(t){}disconnectedCallback(){this._$E_?.forEach((t=>t.hostDisconnected?.()));}attributeChangedCallback(t,s,i){this._$AK(t,i);}_$EO(t,s){const i=this.constructor.elementProperties.get(t),e=this.constructor._$Eu(t,i);if(void 0!==e&&!0===i.reflect){const r=(void 0!==i.converter?.toAttribute?i.converter:u$3).toAttribute(s,i.type);this._$Em=t,null==r?this.removeAttribute(e):this.setAttribute(e,r),this._$Em=null;}}_$AK(t,s){const i=this.constructor,e=i._$Eh.get(t);if(void 0!==e&&this._$Em!==e){const t=i.getPropertyOptions(e),r="function"==typeof t.converter?{fromAttribute:t.converter}:void 0!==t.converter?.fromAttribute?t.converter:u$3;this._$Em=e,this[e]=r.fromAttribute(s,t.type),this._$Em=null;}}requestUpdate(t,s,i,e=!1,r){if(void 0!==t){if(i??=this.constructor.getPropertyOptions(t),!(i.hasChanged??f$1)(e?r:this[t],s))return;this.C(t,s,i);}!1===this.isUpdatePending&&(this._$Eg=this._$EP());}C(t,s,i){this._$AL.has(t)||this._$AL.set(t,s),!0===i.reflect&&this._$Em!==t&&(this._$Ej??=new Set).add(t);}async _$EP(){this.isUpdatePending=!0;try{await this._$Eg;}catch(t){Promise.reject(t);}const t=this.scheduleUpdate();return null!=t&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){if(!this.isUpdatePending)return;if(!this.hasUpdated){if(this.renderRoot??=this.createRenderRoot(),this._$Ep){for(const[t,s]of this._$Ep)this[t]=s;this._$Ep=void 0;}const t=this.constructor.elementProperties;if(t.size>0)for(const[s,i]of t)!0!==i.wrapped||this._$AL.has(s)||void 0===this[s]||this.C(s,this[s],i);}let t=!1;const s=this._$AL;try{t=this.shouldUpdate(s),t?(this.willUpdate(s),this._$E_?.forEach((t=>t.hostUpdate?.())),this.update(s)):this._$ET();}catch(s){throw t=!1,this._$ET(),s}t&&this._$AE(s);}willUpdate(t){}_$AE(t){this._$E_?.forEach((t=>t.hostUpdated?.())),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t);}_$ET(){this._$AL=new Map,this.isUpdatePending=!1;}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$Eg}shouldUpdate(t){return !0}update(t){this._$Ej&&=this._$Ej.forEach((t=>this._$EO(t,this[t]))),this._$ET();}updated(t){}firstUpdated(t){}}b.elementStyles=[],b.shadowRootOptions={mode:"open"},b[d$1("elementProperties")]=new Map,b[d$1("finalized")]=new Map,p$2?.({ReactiveElement:b}),(a$1.reactiveElementVersions??=[]).push("2.0.2");

  /**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   */
  const t$2=globalThis,i$1=t$2.trustedTypes,s$2=i$1?i$1.createPolicy("lit-html",{createHTML:t=>t}):void 0,e$2="$lit$",h$1=`lit$${(Math.random()+"").slice(9)}$`,o$1="?"+h$1,n=`<${o$1}>`,r$2=document,l=()=>r$2.createComment(""),c$1=t=>null===t||"object"!=typeof t&&"function"!=typeof t,a=Array.isArray,u$2=t=>a(t)||"function"==typeof t?.[Symbol.iterator],d="[ \t\n\f\r]",f=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,v$1=/-->/g,_=/>/g,m$1=RegExp(`>|${d}(?:([^\\s"'>=/]+)(${d}*=${d}*(?:[^ \t\n\f\r"'\`<>=]|("|')|))|$)`,"g"),p$1=/'/g,g=/"/g,$=/^(?:script|style|textarea|title)$/i,y=t=>(i,...s)=>({_$litType$:t,strings:i,values:s}),x=y(1),w=Symbol.for("lit-noChange"),T=Symbol.for("lit-nothing"),A=new WeakMap,E=r$2.createTreeWalker(r$2,129);function C(t,i){if(!Array.isArray(t)||!t.hasOwnProperty("raw"))throw Error("invalid template strings array");return void 0!==s$2?s$2.createHTML(i):i}const P=(t,i)=>{const s=t.length-1,o=[];let r,l=2===i?"<svg>":"",c=f;for(let i=0;i<s;i++){const s=t[i];let a,u,d=-1,y=0;for(;y<s.length&&(c.lastIndex=y,u=c.exec(s),null!==u);)y=c.lastIndex,c===f?"!--"===u[1]?c=v$1:void 0!==u[1]?c=_:void 0!==u[2]?($.test(u[2])&&(r=RegExp("</"+u[2],"g")),c=m$1):void 0!==u[3]&&(c=m$1):c===m$1?">"===u[0]?(c=r??f,d=-1):void 0===u[1]?d=-2:(d=c.lastIndex-u[2].length,a=u[1],c=void 0===u[3]?m$1:'"'===u[3]?g:p$1):c===g||c===p$1?c=m$1:c===v$1||c===_?c=f:(c=m$1,r=void 0);const x=c===m$1&&t[i+1].startsWith("/>")?" ":"";l+=c===f?s+n:d>=0?(o.push(a),s.slice(0,d)+e$2+s.slice(d)+h$1+x):s+h$1+(-2===d?i:x);}return [C(t,l+(t[s]||"<?>")+(2===i?"</svg>":"")),o]};class V{constructor({strings:t,_$litType$:s},n){let r;this.parts=[];let c=0,a=0;const u=t.length-1,d=this.parts,[f,v]=P(t,s);if(this.el=V.createElement(f,n),E.currentNode=this.el.content,2===s){const t=this.el.content.firstChild;t.replaceWith(...t.childNodes);}for(;null!==(r=E.nextNode())&&d.length<u;){if(1===r.nodeType){if(r.hasAttributes())for(const t of r.getAttributeNames())if(t.endsWith(e$2)){const i=v[a++],s=r.getAttribute(t).split(h$1),e=/([.?@])?(.*)/.exec(i);d.push({type:1,index:c,name:e[2],strings:s,ctor:"."===e[1]?k:"?"===e[1]?H:"@"===e[1]?I:R}),r.removeAttribute(t);}else t.startsWith(h$1)&&(d.push({type:6,index:c}),r.removeAttribute(t));if($.test(r.tagName)){const t=r.textContent.split(h$1),s=t.length-1;if(s>0){r.textContent=i$1?i$1.emptyScript:"";for(let i=0;i<s;i++)r.append(t[i],l()),E.nextNode(),d.push({type:2,index:++c});r.append(t[s],l());}}}else if(8===r.nodeType)if(r.data===o$1)d.push({type:2,index:c});else {let t=-1;for(;-1!==(t=r.data.indexOf(h$1,t+1));)d.push({type:7,index:c}),t+=h$1.length-1;}c++;}}static createElement(t,i){const s=r$2.createElement("template");return s.innerHTML=t,s}}function N(t,i,s=t,e){if(i===w)return i;let h=void 0!==e?s._$Co?.[e]:s._$Cl;const o=c$1(i)?void 0:i._$litDirective$;return h?.constructor!==o&&(h?._$AO?.(!1),void 0===o?h=void 0:(h=new o(t),h._$AT(t,s,e)),void 0!==e?(s._$Co??=[])[e]=h:s._$Cl=h),void 0!==h&&(i=N(t,h._$AS(t,i.values),h,e)),i}class S{constructor(t,i){this._$AV=[],this._$AN=void 0,this._$AD=t,this._$AM=i;}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}u(t){const{el:{content:i},parts:s}=this._$AD,e=(t?.creationScope??r$2).importNode(i,!0);E.currentNode=e;let h=E.nextNode(),o=0,n=0,l=s[0];for(;void 0!==l;){if(o===l.index){let i;2===l.type?i=new M(h,h.nextSibling,this,t):1===l.type?i=new l.ctor(h,l.name,l.strings,this,t):6===l.type&&(i=new L(h,this,t)),this._$AV.push(i),l=s[++n];}o!==l?.index&&(h=E.nextNode(),o++);}return E.currentNode=r$2,e}p(t){let i=0;for(const s of this._$AV)void 0!==s&&(void 0!==s.strings?(s._$AI(t,s,i),i+=s.strings.length-2):s._$AI(t[i])),i++;}}class M{get _$AU(){return this._$AM?._$AU??this._$Cv}constructor(t,i,s,e){this.type=2,this._$AH=T,this._$AN=void 0,this._$AA=t,this._$AB=i,this._$AM=s,this.options=e,this._$Cv=e?.isConnected??!0;}get parentNode(){let t=this._$AA.parentNode;const i=this._$AM;return void 0!==i&&11===t?.nodeType&&(t=i.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,i=this){t=N(this,t,i),c$1(t)?t===T||null==t||""===t?(this._$AH!==T&&this._$AR(),this._$AH=T):t!==this._$AH&&t!==w&&this._(t):void 0!==t._$litType$?this.g(t):void 0!==t.nodeType?this.$(t):u$2(t)?this.T(t):this._(t);}k(t){return this._$AA.parentNode.insertBefore(t,this._$AB)}$(t){this._$AH!==t&&(this._$AR(),this._$AH=this.k(t));}_(t){this._$AH!==T&&c$1(this._$AH)?this._$AA.nextSibling.data=t:this.$(r$2.createTextNode(t)),this._$AH=t;}g(t){const{values:i,_$litType$:s}=t,e="number"==typeof s?this._$AC(t):(void 0===s.el&&(s.el=V.createElement(C(s.h,s.h[0]),this.options)),s);if(this._$AH?._$AD===e)this._$AH.p(i);else {const t=new S(e,this),s=t.u(this.options);t.p(i),this.$(s),this._$AH=t;}}_$AC(t){let i=A.get(t.strings);return void 0===i&&A.set(t.strings,i=new V(t)),i}T(t){a(this._$AH)||(this._$AH=[],this._$AR());const i=this._$AH;let s,e=0;for(const h of t)e===i.length?i.push(s=new M(this.k(l()),this.k(l()),this,this.options)):s=i[e],s._$AI(h),e++;e<i.length&&(this._$AR(s&&s._$AB.nextSibling,e),i.length=e);}_$AR(t=this._$AA.nextSibling,i){for(this._$AP?.(!1,!0,i);t&&t!==this._$AB;){const i=t.nextSibling;t.remove(),t=i;}}setConnected(t){void 0===this._$AM&&(this._$Cv=t,this._$AP?.(t));}}class R{get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}constructor(t,i,s,e,h){this.type=1,this._$AH=T,this._$AN=void 0,this.element=t,this.name=i,this._$AM=e,this.options=h,s.length>2||""!==s[0]||""!==s[1]?(this._$AH=Array(s.length-1).fill(new String),this.strings=s):this._$AH=T;}_$AI(t,i=this,s,e){const h=this.strings;let o=!1;if(void 0===h)t=N(this,t,i,0),o=!c$1(t)||t!==this._$AH&&t!==w,o&&(this._$AH=t);else {const e=t;let n,r;for(t=h[0],n=0;n<h.length-1;n++)r=N(this,e[s+n],i,n),r===w&&(r=this._$AH[n]),o||=!c$1(r)||r!==this._$AH[n],r===T?t=T:t!==T&&(t+=(r??"")+h[n+1]),this._$AH[n]=r;}o&&!e&&this.O(t);}O(t){t===T?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,t??"");}}class k extends R{constructor(){super(...arguments),this.type=3;}O(t){this.element[this.name]=t===T?void 0:t;}}class H extends R{constructor(){super(...arguments),this.type=4;}O(t){this.element.toggleAttribute(this.name,!!t&&t!==T);}}class I extends R{constructor(t,i,s,e,h){super(t,i,s,e,h),this.type=5;}_$AI(t,i=this){if((t=N(this,t,i,0)??T)===w)return;const s=this._$AH,e=t===T&&s!==T||t.capture!==s.capture||t.once!==s.once||t.passive!==s.passive,h=t!==T&&(s===T||e);e&&this.element.removeEventListener(this.name,this,s),h&&this.element.addEventListener(this.name,this,t),this._$AH=t;}handleEvent(t){"function"==typeof this._$AH?this._$AH.call(this.options?.host??this.element,t):this._$AH.handleEvent(t);}}class L{constructor(t,i,s){this.element=t,this.type=6,this._$AN=void 0,this._$AM=i,this.options=s;}get _$AU(){return this._$AM._$AU}_$AI(t){N(this,t);}}const z={j:e$2,P:h$1,A:o$1,C:1,M:P,L:S,R:u$2,V:N,D:M,I:R,H,N:I,U:k,B:L},Z=t$2.litHtmlPolyfillSupport;Z?.(V,M),(t$2.litHtmlVersions??=[]).push("3.1.0");const j=(t,i,s)=>{const e=s?.renderBefore??i;let h=e._$litPart$;if(void 0===h){const t=s?.renderBefore??null;e._$litPart$=h=new M(i.insertBefore(l(),t),t,void 0,s??{});}return h._$AI(t),h};

  /**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   */let s$1 = class s extends b{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0;}createRenderRoot(){const t=super.createRenderRoot();return this.renderOptions.renderBefore??=t.firstChild,t}update(t){const i=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this._$Do=j(i,this.renderRoot,this.renderOptions);}connectedCallback(){super.connectedCallback(),this._$Do?.setConnected(!0);}disconnectedCallback(){super.disconnectedCallback(),this._$Do?.setConnected(!1);}render(){return w}};s$1._$litElement$=!0,s$1[("finalized")]=!0,globalThis.litElementHydrateSupport?.({LitElement:s$1});const r$1=globalThis.litElementPolyfillSupport;r$1?.({LitElement:s$1});(globalThis.litElementVersions??=[]).push("4.0.2");

  /**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   */
  const t$1={ATTRIBUTE:1,CHILD:2,PROPERTY:3,BOOLEAN_ATTRIBUTE:4,EVENT:5,ELEMENT:6},e$1=t=>(...e)=>({_$litDirective$:t,values:e});class i{constructor(t){}get _$AU(){return this._$AM._$AU}_$AT(t,e,i){this._$Ct=t,this._$AM=e,this._$Ci=i;}_$AS(t,e){return this.update(t,e)}update(t,e){return this.render(...e)}}

  /**
   * @license
   * Copyright 2020 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   */const {D:t}=z,s=()=>document.createComment(""),r=(o,i,n)=>{const e=o._$AA.parentNode,l=void 0===i?o._$AB:i._$AA;if(void 0===n){const i=e.insertBefore(s(),l),c=e.insertBefore(s(),l);n=new t(i,c,o,o.options);}else {const t=n._$AB.nextSibling,i=n._$AM,c=i!==o;if(c){let t;n._$AQ?.(o),n._$AM=o,void 0!==n._$AP&&(t=o._$AU)!==i._$AU&&n._$AP(t);}if(t!==l||c){let o=n._$AA;for(;o!==t;){const t=o.nextSibling;e.insertBefore(o,l),o=t;}}}return n},v=(o,t,i=o)=>(o._$AI(t,i),o),u$1={},m=(o,t=u$1)=>o._$AH=t,p=o=>o._$AH,h=o=>{o._$AP?.(!1,!0);let t=o._$AA;const i=o._$AB.nextSibling;for(;t!==i;){const o=t.nextSibling;t.remove(),t=o;}};

  /**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   */
  const u=(e,s,t)=>{const r=new Map;for(let l=s;l<=t;l++)r.set(e[l],l);return r},c=e$1(class extends i{constructor(e){if(super(e),e.type!==t$1.CHILD)throw Error("repeat() can only be used in text expressions")}ht(e,s,t){let r;void 0===t?t=s:void 0!==s&&(r=s);const l=[],o=[];let i=0;for(const s of e)l[i]=r?r(s,i):i,o[i]=t(s,i),i++;return {values:o,keys:l}}render(e,s,t){return this.ht(e,s,t).values}update(s,[t,r$1,c]){const d=p(s),{values:p$1,keys:a}=this.ht(t,r$1,c);if(!Array.isArray(d))return this.dt=a,p$1;const h$1=this.dt??=[],v$1=[];let m$1,y,x=0,j=d.length-1,k=0,w$1=p$1.length-1;for(;x<=j&&k<=w$1;)if(null===d[x])x++;else if(null===d[j])j--;else if(h$1[x]===a[k])v$1[k]=v(d[x],p$1[k]),x++,k++;else if(h$1[j]===a[w$1])v$1[w$1]=v(d[j],p$1[w$1]),j--,w$1--;else if(h$1[x]===a[w$1])v$1[w$1]=v(d[x],p$1[w$1]),r(s,v$1[w$1+1],d[x]),x++,w$1--;else if(h$1[j]===a[k])v$1[k]=v(d[j],p$1[k]),r(s,d[x],d[j]),j--,k++;else if(void 0===m$1&&(m$1=u(a,k,w$1),y=u(h$1,x,j)),m$1.has(h$1[x]))if(m$1.has(h$1[j])){const e=y.get(a[k]),t=void 0!==e?d[e]:null;if(null===t){const e=r(s,d[x]);v(e,p$1[k]),v$1[k]=e;}else v$1[k]=v(t,p$1[k]),r(s,d[x],t),d[e]=null;k++;}else h(d[j]),j--;else h(d[x]),x++;for(;k<=w$1;){const e=r(s,v$1[w$1+1]);v(e,p$1[k]),v$1[k++]=e;}for(;x<=j;){const e=d[x++];null!==e&&h(e);}return this.dt=a,m(s,v$1),w}});

  /**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   */class e extends i{constructor(i){if(super(i),this.et=T,i.type!==t$1.CHILD)throw Error(this.constructor.directiveName+"() can only be used in child bindings")}render(r){if(r===T||null==r)return this.vt=void 0,this.et=r;if(r===w)return r;if("string"!=typeof r)throw Error(this.constructor.directiveName+"() called with a non-string value");if(r===this.et)return this.vt;this.et=r;const s=[r];return s.raw=s,this.vt={_$litType$:this.constructor.resultType,strings:s,values:[]}}}e.directiveName="unsafeHTML",e.resultType=1;const o=e$1(e);

  const COLOR_TODAY = 'gold';
  const COLOR_TOMORROW = 'lightgreen';

  class AppCalendarEvent extends s$1 {
    static get properties() {
      return {
        event: { attribute: null }
      }
    }

    constructor() {
      super();
      this.event = {};
    }

    firstUpdated() {
      this.#adjustSummaryFontSize();
    }

    #adjustSummaryFontSize() {
      const summary = this.shadowRoot.querySelector('.summary');
      if (summary.scrollWidth / summary.clientWidth > 1.25) {
        // Squeezing onto one line will be too small. Reduce the
        // font size and allow wrapping.
        const fontSize = parseFloat(getComputedStyle(summary).fontSize);
        // @ts-ignore
        summary.style.fontSize = (fontSize * 0.8) + 'px';
        // @ts-ignore
        summary.style.whiteSpace = 'normal';
      } else {
        // Scale the font size down until the line fits.
        while (summary.scrollWidth / summary.clientWidth > 1) {
          const fontSize = parseFloat(getComputedStyle(summary).fontSize);
          // @ts-ignore
          summary.style.fontSize = (fontSize * 0.99) + 'px';
        }
        }
    }

    #getDescription() {
      const description = ((description) => {
        if (description.startsWith('<html-blob>')) {
          return new DOMParser()
            .parseFromString(this.event.description, 'text/xml')
            .documentElement
            .textContent;
        }
        return description;
      })(this.event.description ?? '');

      const html = new DOMParser()
        .parseFromString(description, 'text/html')
        .body
        .innerHTML;
      return o(html);
    }

    #getDate() {
      const date = new Date(this.event.start.dateTime || (this.event.start.date + 'T00:00'));
      const today = new Date().setHours(0, 0, 0, 0);
      switch(Math.trunc((date.valueOf() - today) / 86_400_000)) {
        case 0:
          this.style.backgroundColor = COLOR_TODAY;
          return 'Today';
        case 1:
          this.style.backgroundColor = COLOR_TOMORROW;
          return 'Tomorrow';
        default:
          this.style.backgroundColor = null;
          return date.toLocaleDateString(undefined, {
            weekday: 'long',
            month: 'short',
            day: 'numeric'
          });
      }
    }

    #getTime() {
      const date = new Date(this.event.start.dateTime || (this.event.start.date + 'T00:00'));
      return date.toLocaleTimeString(undefined, { timeStyle: 'short' });

    }

    static get styles() {
      return i$3`
      :host {
        color: black;
        background-color: lightblue;
        font-size: 2vw;
        padding-left: 0.25em;
        padding-right: 0.25em;

        display: flex;
        flex-direction: column;        
      }

      .today {
        background-color: gold;
      }

      .tomorrow {
        background-color: lightgreen;
      }

      .summary {
        font-weight: bold;
        font-size: 2.5vw;
        padding-bottom: 0.1em;
        white-space: nowrap;
      }

      .description {
        flex-grow: 1;
      }

      .footer {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
      }
    `;
    }

    render() {
      return x`
      <div class="summary">${this.event.summary}</div>
      <div class="description">${this.#getDescription()}</div>
      <div class="footer">
        <div class="date">${this.#getDate()}</div>
        <div class="time">${this.#getTime()}</div>
      </div>
    `;
    }
  }
  customElements.define('app-calendar-event', AppCalendarEvent);

  // https://developer.android.com/develop/ui/views/layout/webapps/load-local-content#assetloader
  const ANDROID_APP = 'https://appassets.androidplatform.net';

  const isAndroidApp = (function() {
    const hasAssetLoaderLocation = window.location.href.startsWith(ANDROID_APP);
    return () => hasAssetLoaderLocation;
  })();

  async function getAndroidResource(name) {
    const response = await fetch(`${ANDROID_APP}/x/${name}`);
    return response.text();
  }

  async function getAccessToken() {
    return getAndroidResource('accessToken');
  }

  const {
    GOOGLE_DISCOVERY_DOCS,
    GOOGLE_GAPI_LIBRARIES
  } = JSON.parse(document.getElementById('google-config').textContent);

  const GAPI_URL = 'https://apis.google.com/js/api.js';
  const GIS_URL = 'https://accounts.google.com/gsi/client';

  const withGAPI = (function() {
    const ready = loadScript(GAPI_URL).then(async () => {
      // Initialize GAPI client.
      await new Promise((callback, onerror) => {
        gapi.load(GOOGLE_GAPI_LIBRARIES, { callback, onerror });
      });
      await gapi.client.init({});
      await Promise.all(GOOGLE_DISCOVERY_DOCS.map(discoveryDoc => {
        return gapi.client.load(discoveryDoc);
      }));

      // In the Android app, access tokens are provided by the app
      // asset loader.
      if (isAndroidApp()) return getAccessToken;

      // Outside the app, use Google Identity Services implicit flow.
      // https://developers.google.com/identity/oauth2/web/guides/migration-to-gis#implicit_flow_examples
      const [ config ] = await Promise.all([
        fetch('/test.json').then(response => response.json()),
        loadScript(GIS_URL)
      ]);
      const tokenClient = google.accounts.oauth2.initTokenClient({
        client_id: config.webClientId,
        scope: config.scopes.join(' '),
        prompt: '',
        callback: ''
      });

      return () => new Promise((resolve, reject) => {
        try {
          tokenClient.callback = response => {
            if (response.error) {
              reject(response.error);
            } else {
              resolve(response.access_token);
              console.log('access token received');
            }
          };
          tokenClient.requestAccessToken();
        } catch (e) {
          // Handle errors that are not authorization errors.
          reject(e);
        }
      });
    });

    return async function withGAPI(f) {
      const getToken = await ready;
      for (let i = 0; i < 2; ++i) {
        try {
          return await f(gapi);
        } catch (e) {
          // If the first try fails with an authorization error, get a
          // new token and try again.
          if (!i && needsAuthorization(e)) {
            const token = await getToken();
            gapi.auth.setToken({ access_token: token });
            continue;
          }
          throw e;
        }
      }
    }
  })();

  async function loadScript(url) {
    const script = document.createElement('script');
    script.src = url;
    document.head.appendChild(script);

    await new Promise(resolve => {
      script.addEventListener('load', resolve);
    });
  }

  function needsAuthorization(e) {
    return [401, 403].includes(e.result?.error?.code);
  }

  const CALENDAR_POLL_INTERVAL = 300_000;
  const CALENDAR_POLL_DURATION = 7 * 24 * 60 * 60 * 1000;

  class AppCalendar extends s$1 {
    static get properties() {
      return {
        dateHeader: { state: true },
        timeHeader: { state: true },
        events: { state: true }
      }
    }

    constructor() {
      super();
      this.dateHeader = '';
      this.timeHeader = '';
      this.events = [];

      this.#updateDate();
      this.#updateEvents();
    }

    #updateDate() {
      const date = new Date();
      this.dateHeader = date.toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' });
      this.timeHeader = date.toLocaleTimeString(undefined, { timeStyle: 'short' }).toLowerCase();
      setTimeout(() => this.#updateDate(), 1000);
    }

    async #updateEvents() {
      try {
        const events = await withGAPI(async gapi => {
          // Get the calendars of interest.
          const calendars = await gapi.client.calendar.calendarList.list({}).then(response => {
            return response.result.items.filter(calendar => {
              return calendar.selected &&
                     ['owner', 'writer'].includes(calendar.accessRole);
            });
          });

          // Fetch calendar events.
          const startTime = new Date().setHours(0,0,0,0);
          const endTime = startTime + CALENDAR_POLL_DURATION;
          return Promise.all(calendars.map(async calendar => {
            const response = await gapi.client.calendar.events.list({
              calendarId: calendar.id,
              orderBy: 'startTime',
              singleEvents: true,
              timeMin: new Date(startTime).toISOString(),
              timeMax: new Date(endTime).toISOString()
            });
            return response.result.items;
          }));
        });

        this.events = events
          .flat()
          .sort((a, b) => {
            // Order by start time.
            const aTime = a.start.dateTime || a.start.date;
            const bTime = b.start.dateTime || b.start.date;
            return aTime.localeCompare(bTime)
          })
          .filter((() => {
            // Remove recurring events after the first instance.
            const recurring = new Set();
            return event => {
              if (recurring.has(event.recurringEventId)) {
                return false;
              }
              recurring.add(event.recurringEventId || '');
              return true;
            }
          })())
          .filter((_, i) => i < 9);
      } finally {
        setTimeout(() => this.#updateEvents(), CALENDAR_POLL_INTERVAL);
      }
    }

    static get styles() {
      return i$3`
      :host {
        background-color: black;
        color: white;

        display: flex;
        flex-direction: column;
      }

      .header {
        display: flex;
        flex-direction: row;
        justify-content: space-between;

        font-size: 7.5vw;
        padding: 1rem;
      }

      .content {
        flex-grow: 1;

        display: grid;
        grid-template-columns: repeat(3, 1fr);
        grid-template-rows: repeat(3, 1fr);
        grid-auto-columns: 0;
        grid-auto-rows: 0;
        grid-auto-flow: column;
        gap: 0.5rem;
      }

      .content > * {
        min-width: 0;
        min-height: 0;
      }
    `;
    }

    render() {
      return x`
      <div class="header">
        <div>${this.dateHeader}</div>
        <div>${this.timeHeader}</div>
      </div>
      <div class="content">
        ${c(this.events, event => x`
          <app-calendar-event .event=${event}></app-calendar-event>
        `)}
      </div>
    `;
    }
  }
  customElements.define('app-calendar', AppCalendar);

  const UPDATE_INTERVAL = 60_000;
  const DAYLIGHT_RANGES = [
    [[7, 0, 0, 0],[19, 0, 0, 0]],
  ];

  class AppMain extends s$1 {
    static get properties() {
      return {
        example: { attribute: null }
      }
    }

    constructor() {
      super();
    }

    firstUpdated() {
      this.#updateApp();
    }

    #updateApp() {
      if (this.#isDaylight(new Date())) {
        this.#show('calendar');
      } else {
        this.#show('blackout');
      }

      setTimeout(() => this.#updateApp(), UPDATE_INTERVAL);
    }

    #isDaylight(date) {
      for (const [startTime, endTime] of DAYLIGHT_RANGES) {
        const startDate = new Date(date);
        // @ts-ignore
        startDate.setHours(...startTime);
        
        const endDate = new Date(date);
        // @ts-ignore
        endDate.setHours(...endTime);
    
    
        if (date >= startDate && date < endDate) {
          return true;
        }
      }
      return false;
    }
    
    #show(id) {
      const containers = this.shadowRoot.querySelectorAll('.container');
      // @ts-ignore
      for (const container of containers) {
        if (container.classList.contains('retiring')) {
          container.classList.remove('retiring');
        }

        if (container.id === id) {
          container.classList.add('foreground');
        } else if (container.classList.contains('foreground')) {
          container.classList.remove('foreground');
          container.classList.add('retiring');
        }
      }  }

    static get styles() {
      return i$3`
      :host {
        width: 100%;
        height: 100%;
        display: block;
        overflow: hidden;
      }

      .container {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        overflow: hidden;

        z-index: 0;
        opacity: 0;
      }

      .retiring {
        opacity: 1;
      }

      @keyframes fade-in {
        from { opacity: 0; }
        to { opacity: 1; }
      }

      .foreground {
        z-index: 1;
        opacity: 1;
        animation: fade-in 1s;
      }

      #blackout {
        background-color: black;
      }
    `;
    }

    render() {
      return x`
      <div id="blackout" class="container"></div>
      <app-calendar id="calendar" class="container"></app-calendar>
    `;
    }
  }
  customElements.define('app-main', AppMain);

})();
//# sourceMappingURL=app-main.js.map
