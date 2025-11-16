package org.vish.hollowdemo.api;

import com.netflix.hollow.api.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class MovieDelegateLookupImpl extends HollowObjectAbstractDelegate implements MovieDelegate {

    private final MovieTypeAPI typeAPI;

    public MovieDelegateLookupImpl(MovieTypeAPI typeAPI) {
        this.typeAPI = typeAPI;
    }

    public long getId(int ordinal) {
        return typeAPI.getId(ordinal);
    }

    public Long getIdBoxed(int ordinal) {
        return typeAPI.getIdBoxed(ordinal);
    }

    public int getTitleOrdinal(int ordinal) {
        return typeAPI.getTitleOrdinal(ordinal);
    }

    public int getGenreOrdinal(int ordinal) {
        return typeAPI.getGenreOrdinal(ordinal);
    }

    public double getRating(int ordinal) {
        return typeAPI.getRating(ordinal);
    }

    public Double getRatingBoxed(int ordinal) {
        return typeAPI.getRatingBoxed(ordinal);
    }

    public int getReleaseYear(int ordinal) {
        return typeAPI.getReleaseYear(ordinal);
    }

    public Integer getReleaseYearBoxed(int ordinal) {
        return typeAPI.getReleaseYearBoxed(ordinal);
    }

    public int getDuration(int ordinal) {
        return typeAPI.getDuration(ordinal);
    }

    public Integer getDurationBoxed(int ordinal) {
        return typeAPI.getDurationBoxed(ordinal);
    }

    public int getDescriptionOrdinal(int ordinal) {
        return typeAPI.getDescriptionOrdinal(ordinal);
    }

    public int getDirectorOrdinal(int ordinal) {
        return typeAPI.getDirectorOrdinal(ordinal);
    }

    public int getCastOrdinal(int ordinal) {
        return typeAPI.getCastOrdinal(ordinal);
    }

    public int getWritersOrdinal(int ordinal) {
        return typeAPI.getWritersOrdinal(ordinal);
    }

    public int getProductionCompanyOrdinal(int ordinal) {
        return typeAPI.getProductionCompanyOrdinal(ordinal);
    }

    public int getCountryOrdinal(int ordinal) {
        return typeAPI.getCountryOrdinal(ordinal);
    }

    public int getLanguageOrdinal(int ordinal) {
        return typeAPI.getLanguageOrdinal(ordinal);
    }

    public long getBudget(int ordinal) {
        return typeAPI.getBudget(ordinal);
    }

    public Long getBudgetBoxed(int ordinal) {
        return typeAPI.getBudgetBoxed(ordinal);
    }

    public long getBoxOffice(int ordinal) {
        return typeAPI.getBoxOffice(ordinal);
    }

    public Long getBoxOfficeBoxed(int ordinal) {
        return typeAPI.getBoxOfficeBoxed(ordinal);
    }

    public int getTagsOrdinal(int ordinal) {
        return typeAPI.getTagsOrdinal(ordinal);
    }

    public int getAgeRatingOrdinal(int ordinal) {
        return typeAPI.getAgeRatingOrdinal(ordinal);
    }

    public int getAwardsOrdinal(int ordinal) {
        return typeAPI.getAwardsOrdinal(ordinal);
    }

    public MovieTypeAPI getTypeAPI() {
        return typeAPI;
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

}