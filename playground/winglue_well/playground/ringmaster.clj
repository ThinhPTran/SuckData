(ns winglue-well.playground.ringmaster
  (:use [winglue-well.ringmaster]))

(comment
  (ns winglue-well.ringmaster)
  (load-file "src/winglue_well/calcs.clj")
  (config/init)
  (def datasrcs (config/get-data-sources @config/tao2-cfg))
  (def testid  {:field "EXAMPLE" :lease "A" :well "A1" :cmpl "L" :dsn :pioneer})
  (def well (get-well-db testid))
  (def flowing-gradient-survey (:flowing-gradient-survey-map well))
  (def flwsurv (calcs/FlowingSurvey well)))
