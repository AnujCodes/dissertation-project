package com.intelligent.invoice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.httpclient.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Cors;
import com.google.cloud.storage.Cors.Origin;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.ImageSource;
import com.google.cloud.vision.v1.Likelihood;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import com.google.cloud.vision.v1.WebDetection;
import com.google.cloud.vision.v1.WebDetection.WebImage;
import com.google.cloud.vision.v1.WebDetection.WebPage;
import com.google.gson.Gson;
import com.intelligent.invoice.dto.Face;
import com.intelligent.invoice.dto.FaceLandmark;
import com.intelligent.invoice.dto.InvoiceEntity;
import com.intelligent.invoice.dto.InvoiceReport;
import com.intelligent.invoice.dto.Label;
import com.intelligent.invoice.dto.Landmark;
import com.intelligent.invoice.dto.LngLat;
import com.intelligent.invoice.dto.Logo;
import com.intelligent.invoice.dto.ReportStatus;
import com.intelligent.invoice.dto.SafeSearch;
import com.intelligent.invoice.dto.Text;
import com.intelligent.invoice.dto.Vertex;
import com.intelligent.invoice.dto.VisionResult;
import com.intelligent.invoice.dto.VisionResultEntity;
import com.intelligent.invoice.dto.Web;
import com.intelligent.invoice.dto.WebEntity;
import com.intelligent.invoice.dto.WebUrl;
import com.intelligent.invoice.repository.InvoiceRepository;
import com.intelligent.invoice.repository.ReportRepository;
import com.intelligent.invoice.service.ProcessInvoice;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

@RestController
@CrossOrigin
public class VisionController {

	private static final String BUCKET_NAME = "vision_demo_2020";
	private Storage storage;
	private ImageAnnotatorSettings imageAnnotatorSettings;
	
	@Autowired
	private InvoiceRepository invoiceRepository;
	
	@Autowired
	private ReportRepository reportRepository;
	
	@Autowired
	private ProcessInvoice processInvoice;
	
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	public VisionController(AppConfig appConfig) {
//		Path serviceAccountFile = Paths.get(appConfig.getServiceAccountFile());

		try (InputStream is = getClass()
			    .getClassLoader().getResourceAsStream(appConfig.getServiceAccountFile())) {
			GoogleCredentials credentials = GoogleCredentials.fromStream(is);
			StorageOptions options = StorageOptions.newBuilder()
					.setCredentials(credentials).build();
			this.storage = options.getService();

			Bucket bucket = this.storage.get(BUCKET_NAME);
			if (bucket == null) {

				Cors cors = Cors.newBuilder().setMaxAgeSeconds(3600)
						.setMethods(Collections.singleton(HttpMethod.PUT))
						.setOrigins(appConfig.getOrigins().stream().map(Origin::of)
								.collect(Collectors.toList()))
						.setResponseHeaders(Arrays.asList("Content-Type",
								"Access-Control-Allow-Origin"))
						.build();

				this.storage.create(BucketInfo.newBuilder(BUCKET_NAME)
						.setCors(Collections.singleton(cors)).build());
			}

			this.imageAnnotatorSettings = ImageAnnotatorSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials))
					.build();

		}
		catch (IOException e) {
			LoggerFactory.getLogger(VisionController.class)
					.error("error constructing VisionController", e);
		}
	}

	@PostMapping("/signurl")
	public SignUrlResponse getSignUrl(@RequestParam("contentType") String contentType, String FileName) {
		String uuid = UUID.randomUUID().toString();
		String url = this.storage.signUrl(
				BlobInfo.newBuilder(BUCKET_NAME, FileName).setContentType(contentType)
						.build(),
				3, TimeUnit.HOURS, SignUrlOption.httpMethod(HttpMethod.PUT),
				SignUrlOption.withContentType()).toString();
		return new SignUrlResponse(uuid, url);
	}
	
	@SuppressWarnings("unused")
	@PostMapping("/image/parse")
	public InvoiceEntity parseInvoice(@RequestParam("imageFile") MultipartFile imageFile) throws IOException {
		
		String FileName = imageFile.getOriginalFilename();
		try {
			SignUrlResponse signUrlResponse = getSignUrl("image/jpeg", FileName);
			String signUrl = signUrlResponse.getUrl();
			String uuid = signUrlResponse.getUuid();
			
			File convFile = new File(System.getProperty("java.io.tmpdir")+"/"+FileName);
			imageFile.transferTo(convFile);
			
			OkHttpClient client = new OkHttpClient();
					MediaType mediaType = MediaType.parse("image/jpeg");
					RequestBody body = com.squareup.okhttp.RequestBody.create(mediaType, convFile);
					Request request = new Request.Builder()
					  .url(signUrl).method("PUT", body)
					  .addHeader("Content-Type", "image/jpeg")
					  .build();
					Response response = client.newCall(request).execute();
			
			HttpClient client2 = new HttpClient();
			
			LOG.info("File uploaded to GCS with Status Code: " + response.code());
			
			Request request2 = new Request.Builder()
					  .url("https://project-vision-267813.df.r.appspot.com/image/ocr?filename="+FileName)
					  .method("GET", null)
					  .addHeader("Content-Type", "image/jpeg")
					  .build();
					Response response2 = client.newCall(request2).execute();
	        
	        String parsedImageJson = response2.body().string();
			
			VisionResultEntity data = new Gson().fromJson(parsedImageJson, VisionResultEntity.class);
			InvoiceEntity invoiceEntity = new InvoiceEntity();
			for(int i=0; i<data.getResponse().size(); i++) {
				for(int j=0; j<data.getResponse().get(i).size(); j++) {
					if(processInvoice.checkForInvoiceId(data.getResponse().get(i).get(j))) {
						if((j+1) < data.getResponse().get(i).size()) {
							invoiceEntity.setInvoiceId(data.getResponse().get(i).get(j+1));
						}
						
					} else if(processInvoice.checkForInvoiceDate(data.getResponse().get(i).get(j))) {
						if((j+1) < data.getResponse().get(i).size()) {
							if(processInvoice.checkValidDate(data.getResponse().get(i).get(j+1)) ) {
								invoiceEntity.setInvoiceDate(data.getResponse().get(i).get(j+1));
							}
						}
						
					} else if(processInvoice.checkForAmount(data.getResponse().get(i).get(j))) {
						if((j+1) < data.getResponse().get(i).size()) {
							invoiceEntity.setCurrency(processInvoice.getCurrency(data.getResponse().get(i).get(j+1)));
							String amount = processInvoice.getAmount(data.getResponse().get(i).get(j+1));
							if (!StringUtils.isEmpty(invoiceEntity.getTotalAmount())) {
								if(Double.parseDouble(amount) > Double.parseDouble(invoiceEntity.getTotalAmount()))
									invoiceEntity.setTotalAmount(amount);
							} else {
								invoiceEntity.setTotalAmount(amount);
							}
						}
						
					}
				}
			}
			
			invoiceRepository.save(invoiceEntity);
			return invoiceEntity;
		} catch(Exception e) {
			LOG.error("Error occured while parsing the image: " + e.getMessage());
		} finally {
			this.storage.delete(BlobId.of(BUCKET_NAME, FileName));
		}
		return null;
		
	}
	
	@PostMapping("/report")
	public InvoiceReport generateReport(@org.springframework.web.bind.annotation.RequestBody InvoiceReport report) throws IOException {
		LOG.info("Recieved report data: " + report.getEmployeeId());
		try {
			report.setStatus(ReportStatus.NEW);
			reportRepository.save(report);
		} catch(Exception e) {
			LOG.error("Invalid Report: ", e.getMessage());
		}
		return report;
	}
	
	@GetMapping("/fetch/report")
	public List<InvoiceReport> getAllReport() throws IOException {
		LOG.info("Recieved request for getAllReport: ");
		Iterable<InvoiceReport> reports = null;
		try {
			reports = reportRepository.findAll();
		} catch(Exception e) {
			LOG.error("Invalid Report: " + e.getMessage());
		}
		List<InvoiceReport> result = new ArrayList<InvoiceReport>();
		reports.forEach(result::add);
		return result;
	}
	
	@PostMapping("/approve/report")
	public InvoiceReport approveReport(@RequestParam("id") long id) throws IOException {
		LOG.info("Recieved request to approve employee report: " + id);
		Optional<InvoiceReport> report = null;
		try {
			report = reportRepository.findById(id);
			report.get().setStatus(ReportStatus.APPROVED);
			reportRepository.save(report.get());
		} catch(Exception e) {
			LOG.error("Invalid Report: " + e.getMessage());
		}
		return report.get();
	}
	


	@PostMapping("/vision")
	public VisionResult vision(@RequestParam("uuid") String uuid) throws IOException {

		try (ImageAnnotatorClient vision = ImageAnnotatorClient
				.create(this.imageAnnotatorSettings)) {

			Image img = Image.newBuilder().setSource(ImageSource.newBuilder()
					.setImageUri("gs://" + BUCKET_NAME + "/" + uuid)).build();

			AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
//					.addFeatures(
//							Feature.newBuilder().setType(Type.FACE_DETECTION).build())
//					.addFeatures(
//							Feature.newBuilder().setType(Type.LANDMARK_DETECTION).build())
//					.addFeatures(
//							Feature.newBuilder().setType(Type.LOGO_DETECTION).build())
//					.addFeatures(Feature.newBuilder().setType(Type.LABEL_DETECTION)
//							.setMaxResults(20).build())
//					.addFeatures(
//							Feature.newBuilder().setType(Type.TEXT_DETECTION).build())
//					.addFeatures(Feature.newBuilder().setType(Type.SAFE_SEARCH_DETECTION)
//							.build())
//					.addFeatures(Feature.newBuilder().setType(Type.WEB_DETECTION)
//							.setMaxResults(10).build())
					.addFeatures(
							Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build())
					.setImage(img).build();

			// More Features:
			// DOCUMENT_TEXT_DETECTION
			// IMAGE_PROPERTIES
			// CROP_HINTS
			// PRODUCT_SEARCH
			// OBJECT_LOCALIZATION

			List<AnnotateImageRequest> requests = new ArrayList<>();
			requests.add(request);

			// Performs label detection on the image file
			BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			VisionResult result = new VisionResult();
			AnnotateImageResponse resp = responses.get(0);

			if (resp.hasError()) {
				result.setError(resp.getError().getMessage());
				return result;
			}

			if (resp.getLabelAnnotationsList() != null) {
				List<Label> labels = new ArrayList<>();
				for (EntityAnnotation ea : resp.getLabelAnnotationsList()) {
					Label l = new Label();
					l.setScore(ea.getScore());
					l.setDescription(ea.getDescription());
					labels.add(l);
				}
				result.setLabels(labels);
			}

			if (resp.getLandmarkAnnotationsList() != null) {
				List<Landmark> landmarks = new ArrayList<>();
				for (EntityAnnotation ea : resp.getLandmarkAnnotationsList()) {
					Landmark l = new Landmark();
					l.setScore(ea.getScore());
					l.setDescription(ea.getDescription());

					if (ea.getBoundingPoly() != null) {
						l.setBoundingPoly(
								ea.getBoundingPoly().getVerticesList().stream().map(v -> {
									Vertex vertex = new Vertex();
									vertex.setX(v.getX());
									vertex.setY(v.getY());
									return vertex;
								}).collect(Collectors.toList()));
					}
					if (ea.getLocationsList() != null) {
						l.setLocations(ea.getLocationsList().stream().map(loc -> {
							LngLat ll = new LngLat();
							ll.setLng(loc.getLatLng().getLongitude());
							ll.setLat(loc.getLatLng().getLatitude());
							return ll;
						}).collect(Collectors.toList()));
					}
					landmarks.add(l);
				}
				result.setLandmarks(landmarks);
			}

			if (resp.getLogoAnnotationsList() != null) {
				List<Logo> logos = new ArrayList<>();
				for (EntityAnnotation ea : resp.getLogoAnnotationsList()) {
					Logo l = new Logo();
					l.setScore(ea.getScore());
					l.setDescription(ea.getDescription());

					if (ea.getBoundingPoly() != null) {
						l.setBoundingPoly(
								ea.getBoundingPoly().getVerticesList().stream().map(v -> {
									Vertex vertex = new Vertex();
									vertex.setX(v.getX());
									vertex.setY(v.getY());
									return vertex;
								}).collect(Collectors.toList()));
					}
					logos.add(l);
				}
				result.setLogos(logos);
			}

			if (resp.getTextAnnotationsList() != null) {
				Set<Text> texts = new HashSet<>();
				for (EntityAnnotation ea : resp.getTextAnnotationsList()) {
					Text t = new Text();
					t.setDescription(ea.getDescription().trim());

					if (ea.getBoundingPoly() != null) {
						t.setBoundingPoly(ea.getBoundingPoly().getVerticesList().stream()
								.filter(Objects::nonNull).map(v -> {
									Vertex vertex = new Vertex();
									vertex.setX(v.getX());
									vertex.setY(v.getY());
									return vertex;
								}).collect(Collectors.toList()));
					}

					texts.add(t);
				}
				result.setTexts(texts);
			}

			if (resp.getFaceAnnotationsList() != null) {
				List<Face> faces = new ArrayList<>();
				for (FaceAnnotation fa : resp.getFaceAnnotationsList()) {
					Face face = new Face();
					face.setRollAngle(fa.getRollAngle());
					face.setPanAngle(fa.getPanAngle());
					face.setTiltAngle(fa.getTiltAngle());
					face.setDetectionConfidence(fa.getDetectionConfidence());
					face.setLandmarkingConfidence(fa.getLandmarkingConfidence());

					face.setJoy(fa.getJoyLikelihood());
					face.setJoyRating(likelihoodToNumber(fa.getJoyLikelihood()));

					face.setSorrow(fa.getSorrowLikelihood());
					face.setSorrowRating(likelihoodToNumber(fa.getSorrowLikelihood()));

					face.setAnger(fa.getAngerLikelihood());
					face.setAngerRating(likelihoodToNumber(fa.getAngerLikelihood()));

					face.setSurprise(fa.getSurpriseLikelihood());
					face.setSurpriseRating(
							likelihoodToNumber(fa.getSurpriseLikelihood()));

					face.setUnderExposed(fa.getUnderExposedLikelihood());
					face.setUnderExposedRating(
							likelihoodToNumber(fa.getUnderExposedLikelihood()));

					face.setBlurred(fa.getBlurredLikelihood());
					face.setBlurredRating(likelihoodToNumber(fa.getBlurredLikelihood()));

					face.setHeadwear(fa.getHeadwearLikelihood());
					face.setHeadwearRating(
							likelihoodToNumber(fa.getHeadwearLikelihood()));

					if (fa.getBoundingPoly() != null) {
						face.setBoundingPoly(
								fa.getBoundingPoly().getVerticesList().stream().map(v -> {
									Vertex vertex = new Vertex();
									vertex.setX(v.getX());
									vertex.setY(v.getY());
									return vertex;
								}).collect(Collectors.toList()));
					}

					if (fa.getFdBoundingPoly() != null) {
						face.setFdBoundingPoly(fa.getFdBoundingPoly().getVerticesList()
								.stream().map(v -> {
									Vertex vertex = new Vertex();
									vertex.setX(v.getX());
									vertex.setY(v.getY());
									return vertex;
								}).collect(Collectors.toList()));
					}

					if (fa.getLandmarksList() != null) {
						face.setLandmarks(fa.getLandmarksList().stream().map(l -> {
							FaceLandmark fl = new FaceLandmark();
							fl.setType(l.getType());
							fl.setX(l.getPosition().getX());
							fl.setY(l.getPosition().getY());
							fl.setZ(l.getPosition().getZ());
							return fl;
						}).collect(Collectors.toList()));
					}

					faces.add(face);
				}
				result.setFaces(faces);
			}

			SafeSearchAnnotation safeSearchAnnotation = resp.getSafeSearchAnnotation();
			if (safeSearchAnnotation != null) {
				SafeSearch safeSearch = new SafeSearch();
				safeSearch.setAdult(safeSearchAnnotation.getAdult());
				safeSearch.setAdultRating(
						likelihoodToNumber(safeSearchAnnotation.getAdult()));
				safeSearch.setMedical(safeSearchAnnotation.getMedical());
				safeSearch.setMedicalRating(
						likelihoodToNumber(safeSearchAnnotation.getMedical()));
				safeSearch.setSpoof(safeSearchAnnotation.getSpoof());
				safeSearch.setSpoofRating(
						likelihoodToNumber(safeSearchAnnotation.getSpoof()));
				safeSearch.setViolence(safeSearchAnnotation.getViolence());
				safeSearch.setViolenceRating(
						likelihoodToNumber(safeSearchAnnotation.getViolence()));

				result.setSafeSearch(safeSearch);
			}

			WebDetection webDetection = resp.getWebDetection();
			if (webDetection != null) {
				Web web = new Web();
				List<WebImage> fullMatchingImagesList = webDetection
						.getFullMatchingImagesList();
				List<WebPage> pagesWithMatchingImagesList = webDetection
						.getPagesWithMatchingImagesList();
				List<WebImage> partialMatchingImagesList = webDetection
						.getPartialMatchingImagesList();
				List<com.google.cloud.vision.v1.WebDetection.WebEntity> webEntitiesList = webDetection
						.getWebEntitiesList();

				if (fullMatchingImagesList != null) {
					web.setFullMatchingImages(fullMatchingImagesList.stream().map(e -> {
						WebUrl wu = new WebUrl();
						wu.setScore(e.getScore());
						wu.setUrl(e.getUrl());
						return wu;
					}).collect(Collectors.toList()));
				}

				if (pagesWithMatchingImagesList != null) {
					web.setPagesWithMatchingImages(
							pagesWithMatchingImagesList.stream().map(e -> {
								WebUrl wu = new WebUrl();
								wu.setScore(e.getScore());
								wu.setUrl(e.getUrl());
								return wu;
							}).collect(Collectors.toList()));
				}

				if (partialMatchingImagesList != null) {
					web.setPartialMatchingImages(
							partialMatchingImagesList.stream().map(e -> {
								WebUrl wu = new WebUrl();
								wu.setScore(e.getScore());
								wu.setUrl(e.getUrl());
								return wu;
							}).collect(Collectors.toList()));
				}

				if (webEntitiesList != null) {
					web.setWebEntities(webEntitiesList.stream().map(e -> {
						if (StringUtils.hasText(e.getDescription())) {
							WebEntity we = new WebEntity();
							we.setDescription(e.getDescription());
							we.setEntityId(e.getEntityId());
							we.setScore(e.getScore());
							return we;
						}
						return null;
					}).filter(Objects::nonNull).collect(Collectors.toList()));
				}

				result.setWeb(web);
			}

			return result;
		}
		finally {
			this.storage.delete(BlobId.of(BUCKET_NAME, uuid));
		}
	}

	private static float likelihoodToNumber(Likelihood likelihood) {
		switch (likelihood) {
		case UNKNOWN:
			return 0f;
		case VERY_UNLIKELY:
			return 0.2f;
		case UNLIKELY:
			return 0.4f;
		case POSSIBLE:
			return 0.6f;
		case LIKELY:
			return 0.8f;
		case VERY_LIKELY:
			return 1f;
		case UNRECOGNIZED:
			return 0f;
		default:
			return 0f;
		}
	}

}
