// 前缀
const animatePrefix = 'animate__animated ';
// 开启随机动画 随机动画值
const animateList: string[] = [
  animatePrefix + 'animate__pulse',
  animatePrefix + 'animate__rubberBand',
  animatePrefix + 'animate__bounceIn',
  animatePrefix + 'animate__bounceInLeft',
  animatePrefix + 'animate__fadeIn',
  animatePrefix + 'animate__fadeInLeft',
  animatePrefix + 'animate__fadeInDown',
  animatePrefix + 'animate__fadeInUp',
  animatePrefix + 'animate__flipInX',
  animatePrefix + 'animate__lightSpeedInLeft',
  animatePrefix + 'animate__rotateInDownLeft',
  animatePrefix + 'animate__rollIn',
  animatePrefix + 'animate__rotateInDownLeft',
  animatePrefix + 'animate__zoomIn',
  animatePrefix + 'animate__zoomInDown',
  animatePrefix + 'animate__slideInLeft',
  animatePrefix + 'animate__lightSpeedIn'
];
// 关闭随机动画后的默认效果
const defaultAnimate = animatePrefix + 'animate__fadeIn';
// 搜索隐藏显示动画
const searchAnimate = {
  enter: '',
  leave: ''
};

// 菜单搜索动画
const menuSearchAnimate = {
  enter: animatePrefix + 'animate__fadeIn',
  leave: animatePrefix + 'animate__fadeOut'
};
// logo动画
const logoAnimate = {
  enter: animatePrefix + 'animate__fadeIn',
  leave: animatePrefix + 'animate__fadeOut'
};

export default {
  animateList,
  defaultAnimate,
  searchAnimate,
  menuSearchAnimate,
  logoAnimate
};
