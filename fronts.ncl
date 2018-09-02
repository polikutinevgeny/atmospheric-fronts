begin
  f  = addfile (getenv("FILE"), "r")

  x = f->HGT_P0_L100_GLL0(26,::-1,:)

  t  = f->TMP_P0_L100_GLL0(26,::-1,:)

  t = t-273.15
  t@units = "C//gpm"
  x@units = ""



  wks_type = "png"
  wks_type@wkWidth = 4000
  wks_type@wkHeight = 3000
  output = str_concat((/"../pics/", getenv("NAME")/))
  wks = gsn_open_wks(wks_type, output)

  ; wks = gsn_open_wks("ps","PRMSL")

  res              = True
  res@gsnMaximize  = True                        ; maximize plot in frame
  res@gsnPaperOrientation = "landscape"
  res@gsnPaperMargin = 0.0
  res@gsnLeftString = ""
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
  ; res@mpMinLatF         = 0
  ; res@mpMaxLatF         = 70
  ; res@mpMinLonF         = 70
  ; res@mpMaxLonF         = 210
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

  res2 = True
  res2@gsnLeftString = ""
  res2@gsnDraw = False
  res2@gsnFrame = False
  res2@cnMaxLevelCount      = 30
  res2@cnLevelSelectionMode = "EqualSpacedLevels"
  res2@cnLineLabelFontHeightF = 0.003

  gres                  = True                ; polyline mods desired
  gres@gsLineThicknessF = 3.0                 ; line thickness
  gres@gsLineColor      = "Black"               ; line color 
  gres@gsLineOpacityF = 1.5

  polyres               = True          ; poly marker mods desired 
  polyres@gsMarkerIndex = 16            ; choose circle as polymarker
  polyres@gsMarkerSizeF = 2.0           ; select size to avoid streaking
  polyres@gsMarkerOpacityF = 0.7
  polyres@gsMarkerColor = (/"blue"/)   ; choose color

  wmsetp("ezf",1)           ; Tell wmap we are using an existing map projection.
  wmsetp("lin",10.0)         ; Line width of front curve.
  wmsetp("fro","cold")
  wmsetp("rev", 0)
  wmsetp("cfc",3)           ; Use blue for the triangles.
  wmsetp("wfc",2)           ; Use red for the bumps.
  wmsetp("swi",0.00)        ; Increase the size of the bumps and triangles.
  wmsetp("smf", 1)
  wmsetp("sig", 0.0001)
  wmsetp("smt", 0.2)
  ; wmsetp("smt", 200.0)
  wmsetp("beg", 0.001)
  wmsetp("end", 0.001)
  wmsetp("bet", 0.015)

  cmap = (/                      \
            (/ 1., 1., 1. /),    \  ; 0 - White background.
            (/ 0., 0., 0. /),    \  ; 1 - Black foreground.
            (/ 1., 0., 0. /),    \  ; 2 - Red.
            (/ 0., 0., 1. /),    \  ; 3 - Blue.
            (/ 0., 1., 1. /),    \  ; 4 - Cyan.
            (/ 1., 0., 1. /),    \  ; 5 - Magenta.
            (/ 0., 0.7, 0. /)     \  ; 6 - Green
         /)
  gsn_define_colormap(wks,cmap)

  ; plot = gsn_csm_contour_map_overlay(wks, t, x, res, res2)
  plot = gsn_csm_contour_map(wks, t, res)
  plot2 = gsn_csm_contour(wks, x, res2)
  overlay(plot, plot2)
  draw(plot)

  ; plot = gsn_csm_contour_map(wks,t,res)   
  ; plot = gsn_csm_contour_map(wks, x, res2)

  ; fmask = asciiread("mask.csv", -1, "string")
  ; do n = 0, dimsizes(fmask) - 1
  ;   fr := str_split(fmask(n), ",")
  ;   y := stringtodouble(fr(::2))
  ;   x := stringtodouble(fr(1::2))
  ;   gsn_polymarker(wks,plot,x,y,polyres)
  ;   polyres@gsMarkerColor = (/"red"/)
  ; end do
  ; frame(wks)

  values_1d = asciiread(getenv("FRONTS"), -1, "string")
  do n = 0, dimsizes(values_1d) - 1
    fr := str_split(values_1d(n), ",")
    t := stringtodouble(fr(0))
    wmsetp("fro","cold")
    if (t.eq.1.0) then
      wmsetp("fro","warm")
    end if
    y := stringtodouble(fr(1::2))
    x := stringtodouble(fr(2::2))
    if ((dimsizes(x).eq.1).and.(dimsizes(y).eq.1)) then
      ; gres@gsLineColor = "Blue"
      ; gres@gsLineOpacityF = 0.5
      ; wmsetp("cfc",2)
      continue
    end if
    ; gsn_polyline(wks, plot, x, y, gres) 
    wmdrft(wks, doubletofloat(y), doubletofloat(x))
  end do

  frame(wks)
  ; plot = gsn_csm_contour_map_overlay(wks, t, x, res, res2)

;---To retrieve the levels NCL used, use "getvalues"
  ; getvalues plot@contour
  ;   "cnLevels" : levels
  ; end getvalues
  ; print("levels = " + levels)

end     