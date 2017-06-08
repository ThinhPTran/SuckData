(ns winglue-well.math)

;; from https://github.com/incanter ...
(defn calc-line
  "Finds value in point x.
   Given:
     f(xl) = yl
     f(xr) = yr
     xl <= x <= xr"
  [xl yl xr yr x]
  (let [xl (double xl)
        yl (double yl)
        xr (double xr)
        yr (double yr)
        x (double x)
        coef (/ (- x xl) (- xr xl))]
    (+ (* (- 1.0 coef) yl)
       (* coef yr))))