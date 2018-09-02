begin
  print(getenv("FILE"))
  f  = addfile (getenv("FILE"), "r")

  x = f->HGT_P0_L100_GLL0(26,::-1,:)

  t  = f->TMP_P0_L100_GLL0(26,::-1,:)

  t = t-273.15
  t@units = "C//gpm"
  x@units = ""


  wks_type = "png"
  wks_type@wkWidth = 4000
  wks_type@wkHeight = 3000
  output = str_concat((/"../mask_pics/", getenv("NAME")/))
  wks = gsn_open_wks(wks_type, output)

  ; wks = gsn_open_wks("ps","PRMSL")

  res              = True
  res@gsnMaximize  = True                        ; maximize plot in frame
  res@gsnPaperOrientation = "landscape"
  res@gsnPaperMargin = 0.0
;
; Draw 20 equally spaced contours between min(x)+CI and max(x)-CI.
; CI = contour interval calculated by NCL
;
  res@cnMaxLevelCount      = 30
  res@cnLevelSelectionMode = "EqualSpacedLevels" ; equally spaced contour levels
  res@cnLineLabelFontHeightF = 0.003

  res@mpFillOn     = True                        ; turn on map fill
  res@mpFillColors = (/ "transparent","transparent",\
                        "gray","transparent"/)   ; color the continents gray

  res@mpProjection = "LambertConformal"
  res@gsnMaskLambertConformal = True
  res@mpGridAndLimbOn = True
  res@mpMinLatF         = 20
  res@mpMaxLatF         = 70
  res@mpMinLonF         = 110
  res@mpMaxLonF         = 210
  res@mpCenterLonF      = (res@mpMinLonF + res@mpMaxLonF) / 2.
  res@gsnAddCyclic = False
  res@gsnFrame = False
  res@lbOrientation = "vertical"

  res@cnFillOn                 = True
  res@cnFillPalette            = "BlueWhiteOrangeRed"
  ; res@cnLinesOn           = False
  res@cnLineThicknessF = 0.5
  res@cnFillOpacityF = 0.7
  res@cnSpanFillPalette = True
  res@tiMainString = "Temperature/geopotential height @900hPa"

  ; res2 = True
  ; res2@cnMaxLevelCount      = 30
  ; res2@cnLevelSelectionMode = "EqualSpacedLevels"
  ; res2@cnLineLabelFontHeightF = 0.003
  ; res2@gsnAddCyclic = False
  ; res2@gsnFrame = False
  res2 = True
  res2@gsnDraw = False
  res2@gsnFrame = False
  res2@cnMaxLevelCount      = 30
  res2@cnLevelSelectionMode = "EqualSpacedLevels"
  res2@cnLineLabelFontHeightF = 0.003
  res@gsnLeftString = ""
  res2@gsnLeftString = ""

  polyres               = True          ; poly marker mods desired 
  polyres@gsMarkerIndex = 16            ; choose circle as polymarker
  polyres@gsMarkerSizeF = 2.0           ; select size to avoid streaking
  polyres@gsMarkerOpacityF = 0.7
  polyres@gsMarkerColor = (/"blue"/)   ; choose color

  ; plot = gsn_csm_contour_map_overlay(wks, t, x, res, res2) 
  plot = gsn_csm_contour_map(wks, t, res)
  plot2 = gsn_csm_contour(wks, x, res2)
  overlay(plot, plot2)
  draw(plot)

  values_1d = asciiread("mask.csv", -1, "string")
  do n = 0, dimsizes(values_1d) - 1
    fr := str_split(values_1d(n), ",")
    y := stringtodouble(fr(::2))
    x := stringtodouble(fr(1::2))
    gsn_polymarker(wks,plot,x,y,polyres)
    polyres@gsMarkerColor = (/"red"/)
  end do
  frame(wks)

end     