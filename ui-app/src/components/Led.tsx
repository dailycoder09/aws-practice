interface Props {
  state?: 'up' | 'down' | 'pending';
}

/** A status light. Only the green one pulses. Decorative: the text next to it carries the meaning. */
export function Led({ state = 'up' }: Props) {
  return <span className={`led led--${state}`} aria-hidden="true" />;
}
