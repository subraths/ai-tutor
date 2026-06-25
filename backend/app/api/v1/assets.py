"""Asset download endpoint (svg + audio bytes)."""
from fastapi import APIRouter, Depends
from fastapi.responses import Response

from app.api.deps import get_asset_repo
from app.core.errors import AssetNotFoundError

router = APIRouter(prefix="/assets", tags=["assets"])


@router.get("/{asset_id}")
async def get_asset(asset_id: str, assets=Depends(get_asset_repo)) -> Response:
    item = await assets.get(asset_id)
    if item is None:
        raise AssetNotFoundError(f"Asset {asset_id} not found")
    data, content_type = item
    return Response(content=data, media_type=content_type)
