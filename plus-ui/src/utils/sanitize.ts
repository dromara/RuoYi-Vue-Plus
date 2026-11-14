const DEFAULT_ALLOWED_TAGS = new Set([
  'a',
  'article',
  'b',
  'blockquote',
  'br',
  'caption',
  'code',
  'col',
  'colgroup',
  'del',
  'div',
  'em',
  'figcaption',
  'figure',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'hr',
  'i',
  'img',
  'li',
  'ol',
  'p',
  'pre',
  's',
  'section',
  'span',
  'strong',
  'sub',
  'sup',
  'table',
  'tbody',
  'td',
  'th',
  'thead',
  'tr',
  'u',
  'ul',
  'video',
  'source'
]);

const URI_ATTRS = new Set(['href', 'src']);
const GLOBAL_ALLOWED_ATTRS = new Set(['class', 'title', 'alt', 'style']);
const TAG_ALLOWED_ATTRS: Record<string, Set<string>> = {
  a: new Set(['href', 'target', 'rel']),
  img: new Set(['src', 'alt', 'title']),
  video: new Set(['src', 'poster', 'controls', 'autoplay', 'muted', 'loop']),
  source: new Set(['src', 'type']),
  td: new Set(['colspan', 'rowspan']),
  th: new Set(['colspan', 'rowspan']),
  col: new Set(['span']),
  colgroup: new Set(['span'])
};

const DANGEROUS_TAGS = new Set([
  'script',
  'style',
  'iframe',
  'object',
  'embed',
  'link',
  'meta',
  'base',
  'form',
  'input',
  'button',
  'textarea',
  'select',
  'option',
  'svg',
  'math'
]);
const SAFE_URL_PATTERN = /^(https?:|mailto:|tel:|\/|#|data:image\/(?:png|jpeg|jpg|gif|webp);base64,)/i;
const ALLOWED_STYLE_PROPS = new Set([
  'background-color',
  'color',
  'font-family',
  'font-size',
  'font-style',
  'font-weight',
  'height',
  'line-height',
  'list-style-type',
  'margin-left',
  'max-width',
  'min-width',
  'padding-left',
  'text-align',
  'text-decoration',
  'text-indent',
  'vertical-align',
  'white-space',
  'width'
]);
const UNSAFE_STYLE_VALUE_PATTERN = /(?:expression\s*\(|url\s*\(|javascript:|vbscript:|data:|@import|behavior:)/i;

const sanitizeUrl = (value: string) => {
  const normalized = value.trim();
  if (!normalized) {
    return '';
  }
  return SAFE_URL_PATTERN.test(normalized) ? normalized : '';
};

const sanitizeStyle = (value: string) => {
  const declarations = value
    .split(';')
    .map(item => item.trim())
    .filter(Boolean);

  const safeDeclarations = declarations.flatMap(declaration => {
    const separatorIndex = declaration.indexOf(':');
    if (separatorIndex <= 0) {
      return [];
    }

    const property = declaration.slice(0, separatorIndex).trim().toLowerCase();
    const propertyValue = declaration.slice(separatorIndex + 1).trim();

    if (!ALLOWED_STYLE_PROPS.has(property) || !propertyValue || UNSAFE_STYLE_VALUE_PATTERN.test(propertyValue)) {
      return [];
    }

    return [`${property}: ${propertyValue}`];
  });

  return safeDeclarations.join('; ');
};

const shouldKeepAttr = (tagName: string, attrName: string) => {
  if (attrName.startsWith('on')) {
    return false;
  }
  if (GLOBAL_ALLOWED_ATTRS.has(attrName)) {
    return true;
  }
  return TAG_ALLOWED_ATTRS[tagName]?.has(attrName) ?? false;
};

const sanitizeElement = (element: Element) => {
  const tagName = element.tagName.toLowerCase();

  if (DANGEROUS_TAGS.has(tagName)) {
    element.remove();
    return;
  }

  if (!DEFAULT_ALLOWED_TAGS.has(tagName)) {
    const parent = element.parentNode;
    if (!parent) {
      element.remove();
      return;
    }
    while (element.firstChild) {
      parent.insertBefore(element.firstChild, element);
    }
    parent.removeChild(element);
    return;
  }

  Array.from(element.attributes).forEach(attr => {
    const attrName = attr.name.toLowerCase();
    if (!shouldKeepAttr(tagName, attrName)) {
      element.removeAttribute(attr.name);
      return;
    }

    if (attrName === 'style') {
      const sanitizedStyle = sanitizeStyle(attr.value);
      if (!sanitizedStyle) {
        element.removeAttribute(attr.name);
        return;
      }
      element.setAttribute(attr.name, sanitizedStyle);
      return;
    }

    if (URI_ATTRS.has(attrName)) {
      const sanitized = sanitizeUrl(attr.value);
      if (!sanitized) {
        element.removeAttribute(attr.name);
        return;
      }
      element.setAttribute(attr.name, sanitized);
    }
  });

  if (tagName === 'a' && element.hasAttribute('target')) {
    element.setAttribute('rel', 'noopener noreferrer');
  }
};

export function sanitizeHtml(html?: string) {
  if (!html) {
    return '';
  }

  const template = document.createElement('template');
  template.innerHTML = html;
  const walker = document.createTreeWalker(template.content, NodeFilter.SHOW_ELEMENT);
  const elements: Element[] = [];

  while (walker.nextNode()) {
    elements.push(walker.currentNode as Element);
  }

  elements.forEach(sanitizeElement);
  return template.innerHTML;
}
