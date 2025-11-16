package org.vish.hollowdemo.api;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.api.custom.HollowTypeAPI;
import com.netflix.hollow.api.objects.delegate.HollowCachedDelegate;

@SuppressWarnings("all")
public class MovieDelegateCachedImpl extends HollowObjectAbstractDelegate implements HollowCachedDelegate, MovieDelegate {

    private final Long id;
    private final int titleOrdinal;
    private final int genreOrdinal;
    private final Double rating;
    private final Integer releaseYear;
    private final Integer duration;
    private final int descriptionOrdinal;
    private final int directorOrdinal;
    private final int castOrdinal;
    private final int writersOrdinal;
    private final int productionCompanyOrdinal;
    private final int countryOrdinal;
    private final int languageOrdinal;
    private final Long budget;
    private final Long boxOffice;
    private final int tagsOrdinal;
    private final int ageRatingOrdinal;
    private final int awardsOrdinal;
    private MovieTypeAPI typeAPI;

    public MovieDelegateCachedImpl(MovieTypeAPI typeAPI, int ordinal) {
        this.id = typeAPI.getIdBoxed(ordinal);
        this.titleOrdinal = typeAPI.getTitleOrdinal(ordinal);
        this.genreOrdinal = typeAPI.getGenreOrdinal(ordinal);
        this.rating = typeAPI.getRatingBoxed(ordinal);
        this.releaseYear = typeAPI.getReleaseYearBoxed(ordinal);
        this.duration = typeAPI.getDurationBoxed(ordinal);
        this.descriptionOrdinal = typeAPI.getDescriptionOrdinal(ordinal);
        this.directorOrdinal = typeAPI.getDirectorOrdinal(ordinal);
        this.castOrdinal = typeAPI.getCastOrdinal(ordinal);
        this.writersOrdinal = typeAPI.getWritersOrdinal(ordinal);
        this.productionCompanyOrdinal = typeAPI.getProductionCompanyOrdinal(ordinal);
        this.countryOrdinal = typeAPI.getCountryOrdinal(ordinal);
        this.languageOrdinal = typeAPI.getLanguageOrdinal(ordinal);
        this.budget = typeAPI.getBudgetBoxed(ordinal);
        this.boxOffice = typeAPI.getBoxOfficeBoxed(ordinal);
        this.tagsOrdinal = typeAPI.getTagsOrdinal(ordinal);
        this.ageRatingOrdinal = typeAPI.getAgeRatingOrdinal(ordinal);
        this.awardsOrdinal = typeAPI.getAwardsOrdinal(ordinal);
        this.typeAPI = typeAPI;
    }

    public long getId(int ordinal) {
        if(id == null)
            return Long.MIN_VALUE;
        return id.longValue();
    }

    public Long getIdBoxed(int ordinal) {
        return id;
    }

    public int getTitleOrdinal(int ordinal) {
        return titleOrdinal;
    }

    public int getGenreOrdinal(int ordinal) {
        return genreOrdinal;
    }

    public double getRating(int ordinal) {
        if(rating == null)
            return Double.NaN;
        return rating.doubleValue();
    }

    public Double getRatingBoxed(int ordinal) {
        return rating;
    }

    public int getReleaseYear(int ordinal) {
        if(releaseYear == null)
            return Integer.MIN_VALUE;
        return releaseYear.intValue();
    }

    public Integer getReleaseYearBoxed(int ordinal) {
        return releaseYear;
    }

    public int getDuration(int ordinal) {
        if(duration == null)
            return Integer.MIN_VALUE;
        return duration.intValue();
    }

    public Integer getDurationBoxed(int ordinal) {
        return duration;
    }

    public int getDescriptionOrdinal(int ordinal) {
        return descriptionOrdinal;
    }

    public int getDirectorOrdinal(int ordinal) {
        return directorOrdinal;
    }

    public int getCastOrdinal(int ordinal) {
        return castOrdinal;
    }

    public int getWritersOrdinal(int ordinal) {
        return writersOrdinal;
    }

    public int getProductionCompanyOrdinal(int ordinal) {
        return productionCompanyOrdinal;
    }

    public int getCountryOrdinal(int ordinal) {
        return countryOrdinal;
    }

    public int getLanguageOrdinal(int ordinal) {
        return languageOrdinal;
    }

    public long getBudget(int ordinal) {
        if(budget == null)
            return Long.MIN_VALUE;
        return budget.longValue();
    }

    public Long getBudgetBoxed(int ordinal) {
        return budget;
    }

    public long getBoxOffice(int ordinal) {
        if(boxOffice == null)
            return Long.MIN_VALUE;
        return boxOffice.longValue();
    }

    public Long getBoxOfficeBoxed(int ordinal) {
        return boxOffice;
    }

    public int getTagsOrdinal(int ordinal) {
        return tagsOrdinal;
    }

    public int getAgeRatingOrdinal(int ordinal) {
        return ageRatingOrdinal;
    }

    public int getAwardsOrdinal(int ordinal) {
        return awardsOrdinal;
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

    public MovieTypeAPI getTypeAPI() {
        return typeAPI;
    }

    public void updateTypeAPI(HollowTypeAPI typeAPI) {
        this.typeAPI = (MovieTypeAPI) typeAPI;
    }

}