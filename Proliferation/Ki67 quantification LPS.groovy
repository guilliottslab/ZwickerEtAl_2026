try {
    def rois = getSelectedObjects().findAll { it.isAnnotation() }

    if (rois.isEmpty()) {
        throw new Exception("Please select at least one ROI annotation first.")
    }

    selectObjects(rois)
    clearDetections()

    runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', '''
    {
      "detectionImage": "DAPI",
      "requestedPixelSizeMicrons": 0.5,
      "backgroundRadiusMicrons": 8.0,
      "medianRadiusMicrons": 0.0,
      "sigmaMicrons": 1.5,
      "minAreaMicrons": 10.0,
      "maxAreaMicrons": 400.0,
      "threshold": 100.0,
      "watershedPostProcess": true,
      "cellExpansionMicrons": 0.0,
      "includeNuclei": true,
      "smoothBoundaries": true,
      "makeMeasurements": true
    }
    ''')

    def positiveClass = getPathClass("Ki67+")
    def negativeClass = getPathClass("Ki67-")
    double ki67Threshold = 520.0

    for (roi in rois) {

        def nuclei = roi.getChildObjects().findAll { it.isDetection() }

        int totalNuclei = 0
        int positiveNuclei = 0

        if (nuclei.isEmpty()) {
            roi.getMeasurementList().putMeasurement("Nucleus", 0)
            roi.getMeasurementList().putMeasurement("Ki67+ nuclei count", 0)
            roi.getMeasurementList().putMeasurement("Ki67+ nuclei %", Double.NaN)
            roi.getMeasurementList().closeList()
            continue
        }

        def firstML = nuclei[0].getMeasurementList()
        def candidateMeasurements = [
            "Nucleus: AF488 mean",
            "Nucleus: AF488 median",
            "Nucleus: AF488 max",
            "Nucleus: AF488 sum"
        ]

        String measurementToUse = null
        for (m in candidateMeasurements) {
            if (firstML.getMeasurementNames().contains(m)) {
                measurementToUse = m
                break
            }
        }

        if (measurementToUse == null) {
            print "No AF488 nuclear measurement found for ROI: " + roi + "\n"
            print "Available measurements in first nucleus:\n"
            for (name in firstML.getMeasurementNames()) {
                print name + "\n"
            }

            roi.getMeasurementList().putMeasurement("Nucleus", nuclei.size())
            roi.getMeasurementList().putMeasurement("Ki67+ nuclei count", Double.NaN)
            roi.getMeasurementList().putMeasurement("Ki67+ nuclei %", Double.NaN)
            roi.getMeasurementList().closeList()
            continue
        }

        print "Using measurement: " + measurementToUse + "\n"

        for (nuc in nuclei) {
            double val = nuc.getMeasurementList().get(measurementToUse)
            if (Double.isNaN(val))
                continue

            totalNuclei++

            if (val >= ki67Threshold) {
                nuc.setPathClass(positiveClass)
                positiveNuclei++
            } else {
                nuc.setPathClass(negativeClass)
            }
        }

        double positivePct = totalNuclei > 0 ? (100.0 * positiveNuclei / totalNuclei) : Double.NaN

        roi.getMeasurementList().putMeasurement("Nucleus", totalNuclei)
        roi.getMeasurementList().putMeasurement("Ki67+ nuclei count", positiveNuclei)
        roi.getMeasurementList().putMeasurement("Ki67+ nuclei %", positivePct)
        roi.getMeasurementList().closeList()

        print "ROI: " + roi + "\n"
        print "  Nucleus = " + totalNuclei + "\n"
        print "  Ki67+ nuclei count = " + positiveNuclei + "\n"
        print "  Ki67+ nuclei % = " + String.format('%.2f', positivePct) + "\n"
    }

    fireHierarchyUpdate()

} catch (Exception e) {
    print "ERROR: " + e.getMessage() + "\n"
    e.printStackTrace()
}