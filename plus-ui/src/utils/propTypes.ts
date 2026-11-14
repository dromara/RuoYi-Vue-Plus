import type { CSSProperties } from 'vue';
import VueTypes, { createTypes, toValidableType, type VueTypesInterface, type VueTypeValidableDef } from 'vue-types';

type PropTypes = VueTypesInterface & {
  readonly style: VueTypeValidableDef<CSSProperties>;
  readonly fieldOption: VueTypeValidableDef<Array<FieldOption>>;
};

const propTypes = createTypes({
  func: undefined,
  bool: undefined,
  string: undefined,
  number: undefined,
  object: undefined,
  integer: undefined
}) as PropTypes;

export default class ProjectTypes extends VueTypes {
  static get style() {
    return toValidableType('style', {
      type: [String, Object],
      default: undefined
    });
  }
}
export { propTypes };
