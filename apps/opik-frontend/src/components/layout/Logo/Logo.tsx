import React from "react";
import { cn } from "@/lib/utils";

type LogoProps = {
  expanded: boolean;
};

const Logo: React.FunctionComponent<LogoProps> = ({ expanded }) => {
  return (
    <img
      className={cn("h-6 object-cover object-left", {
        "w-[24px]": !expanded,
      })}
      src="/images/plaud.svg"
      alt="Plaud logo"
    />
  );
};

export default Logo;
