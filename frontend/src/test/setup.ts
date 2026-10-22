import '@testing-library/jest-dom/vitest';

// jsdom kennt keine Layout-Masse, CodeMirror fragt sie aber ab.
const emptyRects = () => ({ length: 0, item: () => null, [Symbol.iterator]: [][Symbol.iterator] }) as unknown as DOMRectList;
Range.prototype.getClientRects = emptyRects;
Range.prototype.getBoundingClientRect = () => new DOMRect();
